package com.vaguehope.onosendai.ui;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.util.SparseArray;
import android.widget.Toast;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.ConfigUnavailableException;
import com.vaguehope.onosendai.config.InternalColumnType;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.images.HybridBitmapCache;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.images.ImageLoaderUtils;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.storage.DbClient;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.update.AlarmReceiver;
import com.vaguehope.onosendai.util.ExecUtils;
import com.vaguehope.onosendai.util.LogWrapper;

public class MainActivity extends FragmentActivity implements ImageLoader {

	public static final String ARG_FOCUS_COLUMN_ID = "focus_column_id";

	private static final LogWrapper LOG = new LogWrapper("MA");

	private Config conf;
	private ProviderMgr providerMgr;
	private HybridBitmapCache imageCache;
	private ExecutorService exec;

	private ViewPager viewPager;
	private VisiblePageSelectionListener pageSelectionListener;
	private final SparseArray<TweetListFragment> activePages = new SparseArray<TweetListFragment>();

	@Override
	protected void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Prefs prefs = new Prefs(this); // TODO replace config with this.
		if (!prefs.isConfigured()) {
			startActivity(new Intent(getApplicationContext(), SetupActivity.class));
			finish();
			return;
		}

		setContentView(R.layout.activity_main);

		try {
			this.conf = Config.getConfig();
		}
		catch (ConfigUnavailableException e) {
			Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		this.imageCache = new HybridBitmapCache(this, C.MAX_MEMORY_IMAGE_CACHE);
		this.exec = ExecUtils.newBoundedCachedThreadPool(C.IMAGE_LOADER_MAX_THREADS, LOG);

		final float columnWidth = Float.parseFloat(getResources().getString(R.string.column_width));

		// If this becomes too memory intensive, switch to android.support.v4.app.FragmentStatePagerAdapter.
		final SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), this.conf, columnWidth);
		this.viewPager = (ViewPager) findViewById(R.id.pager);
		this.viewPager.setAdapter(sectionsPagerAdapter);
		this.pageSelectionListener = new VisiblePageSelectionListener(columnWidth);
		this.viewPager.setOnPageChangeListener(this.pageSelectionListener);
		showPageFromIntent(getIntent());

		AlarmReceiver.configureAlarm(this); // FIXME be more smart about this?
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
	}

	@Override
	protected void onDestroy () {
		if (this.providerMgr != null) this.providerMgr.shutdown();
		if (this.imageCache != null) this.imageCache.clean();
		if (this.exec != null) this.exec.shutdown();
		disposeDb();
		super.onDestroy();
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
		catch (InterruptedException e) {/**/}
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

	public ExecutorService getExec () {
		return this.exec;
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
		ImageLoaderUtils.loadImage(this.imageCache, req, this.exec);
	}

	public void gotoPage (final int position) {
		this.viewPager.setCurrentItem(position, false);
	}

	private void showPageFromIntent (final Intent intent) {
		if (intent.hasExtra(ARG_FOCUS_COLUMN_ID)) {
			final int pos = this.conf.getColumnPositionById(intent.getIntExtra(ARG_FOCUS_COLUMN_ID, 0));
			if (pos >= 0) gotoPage(pos);
		}
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

}
