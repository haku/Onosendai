package com.vaguehope.onosendai.ui;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.InternalColumnType;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.images.HybridBitmapCache;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.images.ImageLoaderUtils;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.notifications.Notifications;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.storage.DbClient;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.ui.pref.AdvancedPrefFragment;
import com.vaguehope.onosendai.update.AlarmReceiver;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.MultiplexingOnPageChangeListener;
import com.vaguehope.onosendai.util.exec.ExecUtils;
import com.vaguehope.onosendai.util.exec.ExecutorEventListener;
import com.vaguehope.onosendai.widget.SidebarAwareViewPager;

public class MainActivity extends FragmentActivity implements ImageLoader, OnSharedPreferenceChangeListener {

	public static final String ARG_FOCUS_COLUMN_ID = "focus_column_id";

	private static final LogWrapper LOG = new LogWrapper("MA");

	private Prefs prefs;
	private Config conf;
	private ProviderMgr providerMgr;
	private HybridBitmapCache imageCache;
	private ExecutorStatus executorStatus;
	private ExecutorService localEs;
	private ExecutorService netEs;

	private SidebarAwareViewPager viewPager;
	private VisiblePageSelectionListener pageSelectionListener;
	private final SparseArray<TweetListFragment> activePages = new SparseArray<TweetListFragment>();

	private boolean prefsChanged = false;

	@Override
	protected void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.prefs = new Prefs(getBaseContext());
		if (!this.prefs.isConfigured()) {
			startActivity(new Intent(getApplicationContext(), SetupActivity.class));
			finish();
			return;
		}
		this.prefs.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

		setContentView(R.layout.activity_main);

		try {
			this.conf = this.prefs.asConfig();
		}
		catch (final Exception e) { // No point continuing if any exception.
			DialogHelper.alertAndClose(this, e);
			return;
		}

		this.imageCache = new HybridBitmapCache(this, C.MAX_MEMORY_IMAGE_CACHE);

		if (this.prefs.getSharedPreferences().getBoolean(AdvancedPrefFragment.KEY_THREAD_INSPECTOR, false)) {
			final TextView jobStatus = (TextView) findViewById(R.id.jobStatus);
			jobStatus.setVisibility(View.VISIBLE);
			this.executorStatus = new ExecutorStatus(jobStatus);
		}

		this.localEs = ExecUtils.newBoundedCachedThreadPool(C.LOCAL_MAX_THREADS, new LogWrapper("LES"), this.executorStatus);
		this.netEs = ExecUtils.newBoundedCachedThreadPool(C.NET_MAX_THREADS, new LogWrapper("NES"), this.executorStatus);

		final float columnWidth = Float.parseFloat(getResources().getString(R.string.column_width));

		// If this becomes too memory intensive, switch to android.support.v4.app.FragmentStatePagerAdapter.
		final SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), this.conf, columnWidth);
		this.viewPager = (SidebarAwareViewPager) findViewById(R.id.pager);
		this.pageSelectionListener = new VisiblePageSelectionListener(columnWidth);
		final MultiplexingOnPageChangeListener onPageChangeListener = new MultiplexingOnPageChangeListener(
				this.pageSelectionListener,
				new NotificationClearingPageSelectionListener(this, this.conf));
		this.viewPager.setOnPageChangeListener(onPageChangeListener);
		this.viewPager.setAdapter(sectionsPagerAdapter);
		if (!showPageFromIntent(getIntent())) onPageChangeListener.onPageSelected(this.viewPager.getCurrentItem());

		AlarmReceiver.configureAlarms(this); // FIXME be more smart about this?
	}

	@Override
	protected void onNewIntent (final Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		showPageFromIntent(intent);
	}

	@Override
	public void onResume () {
		super.onResume();
		resumeDb();

		if (this.prefsChanged) {
			DialogHelper.alertAndRun(this, "Activity will be reloaded with new preferences.", new Runnable() {
				@Override
				public void run () {
					finish();
					startActivity(getIntent());
				}
			});
		}
	}

	@Override
	protected void onDestroy () {
		if (this.prefs != null) this.prefs.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		if (this.providerMgr != null) this.providerMgr.shutdown();
		if (this.imageCache != null) this.imageCache.clean();
		if (this.netEs != null) this.netEs.shutdown();
		if (this.localEs != null) this.localEs.shutdown();
		disposeDb();
		super.onDestroy();
	}

	@Override
	public void onSharedPreferenceChanged (final SharedPreferences sharedPreferences, final String key) {
		this.prefsChanged = true;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final CountDownLatch dbReadyLatch = new CountDownLatch(1);
	private DbClient bndDb;

	private void resumeDb () {
		if (this.bndDb == null) {
			LOG.d("Binding DB service...");
			final CountDownLatch latch = this.dbReadyLatch;
			final LogWrapper log = LOG;
			this.bndDb = new DbClient(this, LOG.getPrefix(), new Runnable() {
				@Override
				public void run () {
					latch.countDown();
					log.d("DB service bound.");
					setProviderMgr(new ProviderMgr(getDb()));
				}
			});
		}
	}

	private void disposeDb () {
		if (this.bndDb != null) this.bndDb.dispose();
	}

	private boolean waitForDbReady () {
		boolean dbReady = false;
		try {
			dbReady = this.dbReadyLatch.await(C.DB_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		}
		catch (final InterruptedException e) {/**/}
		if (!dbReady) LOG.e("Not updateing: Time out waiting for DB service to connect.");
		return dbReady;
	}

	protected DbInterface getDb () {
		final DbClient d = this.bndDb;
		if (d == null) return null;
		return d.getDb();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	Config getConf () {
		return this.conf;
	}

	public ExecutorEventListener getExecutorEventListener() {
		return this.executorStatus;
	}

	public Executor getLocalEs () {
		return this.localEs;
	}

	public Executor getNetEs () {
		return this.netEs;
	}

	ProviderMgr getProviderMgr () {
		if (!waitForDbReady()) throw new IllegalStateException("DB not bound.");
		return this.providerMgr;
	}

	void setProviderMgr (final ProviderMgr providerMgr) {
		this.providerMgr = providerMgr;
	}

	@Override
	public void loadImage (final ImageLoadRequest req) {
		ImageLoaderUtils.loadImage(this.imageCache, req, this.localEs, this.netEs, this.executorStatus);
	}

	public boolean gotoPage (final int position) {
		if (this.viewPager.getCurrentItem() != position) {
			this.viewPager.setCurrentItem(position, false);
			return true;
		}
		return false;
	}

	private boolean showPageFromIntent (final Intent intent) {
		if (intent.hasExtra(ARG_FOCUS_COLUMN_ID)) {
			final int pos = this.conf.getColumnPositionById(intent.getIntExtra(ARG_FOCUS_COLUMN_ID, 0));
			if (pos >= 0) return gotoPage(pos);
		}
		return false;
	}

	protected void onFragmentResumed (final int columnId, final TweetListFragment page) {
		this.activePages.put(columnId, page);
	}

	protected void onFragmentPaused (final int columnId) {
		this.activePages.remove(columnId);
	}

	protected ScrollState getColumnScroll (final int columnId) {
		final TweetListFragment page = this.activePages.get(columnId);
		if (page == null) return null;
		return page.getCurrentScroll();
	}

	@Override
	public void onBackPressed () {
		for (int i = 0; i < this.activePages.size(); i++) {
			final TweetListFragment page = this.activePages.valueAt(i);
			if (this.pageSelectionListener.isVisible(page.getColumnPosition()) && page.getSidebar().closeSidebar()) return;
		}
		super.onBackPressed();
	}

	private static class SectionsPagerAdapter extends FragmentPagerAdapter {

		private final Config conf;
		private final float pageWidth;

		public SectionsPagerAdapter (final FragmentManager fm, final Config conf, final float pageWidth) {
			super(fm);
			this.conf = conf;
			this.pageWidth = pageWidth;
		}

		@Override
		public Fragment getItem (final int position) {
			final Column col = this.conf.getColumnByPosition(position);
			final Fragment fragment = new TweetListFragment();
			final Bundle args = new Bundle();
			args.putInt(TweetListFragment.ARG_COLUMN_POSITION, position);
			args.putInt(TweetListFragment.ARG_COLUMN_ID, col.getId());
			args.putString(TweetListFragment.ARG_COLUMN_TITLE, col.getTitle());
			args.putBoolean(TweetListFragment.ARG_COLUMN_IS_LATER, InternalColumnType.LATER.matchesColumn(col));
			args.putString(TweetListFragment.ARG_COLUMN_SHOW_INLINEMEDIA, col.getInlineMediaStyle() != null ? col.getInlineMediaStyle().serialise() : null);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount () {
			return this.conf.getColumns().size();
		}

		@Override
		public CharSequence getPageTitle (final int position) {
			return this.conf.getColumnByPosition(position).getTitle();
		}

		@Override
		public float getPageWidth (final int position) {
			return this.pageWidth;
		}

	}

	private static class VisiblePageSelectionListener extends SimpleOnPageChangeListener {

		private int selectedPagePosition;
		private final int visiblePages;

		public VisiblePageSelectionListener (final float pageWidth) {
			this.visiblePages = (int) (1 / pageWidth);
		}

		@Override
		public void onPageSelected (final int position) {
			this.selectedPagePosition = position;
		}

		public boolean isVisible (final int position) {
			return position >= this.selectedPagePosition && position < this.selectedPagePosition + this.visiblePages;
		}

	}

	private static class NotificationClearingPageSelectionListener extends SimpleOnPageChangeListener {

		private final Context context;
		private final Config conf;

		public NotificationClearingPageSelectionListener (final Context context, final Config conf) {
			this.context = context;
			this.conf = conf;
		}

		@Override
		public void onPageSelected (final int position) {
			final Column col = this.conf.getColumnByPosition(position);
			Notifications.clearColumn(this.context, col);
		}

	}

}
