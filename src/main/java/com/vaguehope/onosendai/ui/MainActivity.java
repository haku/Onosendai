package com.vaguehope.onosendai.ui;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ColumnTitleStrip;
import android.support.v4.view.ColumnTitleStrip.ColumnClickListener;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.InternalColumnType;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.images.HybridBitmapCache;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.images.ImageLoaderUtils;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.notifications.Notifications;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.provider.ProviderMgr.ProviderMgrProvider;
import com.vaguehope.onosendai.storage.DbClient;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.storage.DbProvider;
import com.vaguehope.onosendai.ui.LocalSearchDialog.OnTweetListener;
import com.vaguehope.onosendai.ui.pref.AdvancedPrefFragment;
import com.vaguehope.onosendai.ui.pref.FiltersPrefFragment;
import com.vaguehope.onosendai.ui.pref.OsPreferenceActivity;
import com.vaguehope.onosendai.ui.pref.UiPrefFragment;
import com.vaguehope.onosendai.update.AlarmReceiver;
import com.vaguehope.onosendai.update.BatteryNotify;
import com.vaguehope.onosendai.update.UpdateService;
import com.vaguehope.onosendai.util.BatteryHelper;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.MultiplexingOnPageChangeListener;
import com.vaguehope.onosendai.util.NetHelper;
import com.vaguehope.onosendai.util.exec.ExecUtils;
import com.vaguehope.onosendai.util.exec.ExecutorEventListener;
import com.vaguehope.onosendai.util.exec.TrackingAsyncTask;
import com.vaguehope.onosendai.widget.SidebarAwareViewPager;

public class MainActivity extends FragmentActivity implements ImageLoader, DbProvider, ProviderMgrProvider, OnSharedPreferenceChangeListener {

	public static final String ARG_FOCUS_COLUMN_ID = "focus_column_id";

	private static final LogWrapper LOG = new LogWrapper("MA");

	private Prefs prefs;
	private Config conf;
	private ProviderMgr providerMgr;
	private HybridBitmapCache imageCache;
	private ExecutorStatus executorStatus;
	private ExecutorService localEs;
	private ExecutorService netEs;
	private MessageHandler msgHandler;
	private boolean columnsRtl;

	private ColumnTitleStrip columnTitleStrip;
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

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);

		try {
			this.conf = this.prefs.asConfig();
		}
		catch (final Exception e) { // No point continuing if any exception.
			LOG.wtf("Unparsable config.", e);
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
		this.msgHandler = new MessageHandler(this);

		final float columnWidth = UiPrefFragment.readColumnWidth(this, this.prefs);
		this.columnsRtl = UiPrefFragment.readColumnsRtl(this.prefs);

		// If this becomes too memory intensive, switch to android.support.v4.app.FragmentStatePagerAdapter.
		final SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), this, columnWidth);
		this.viewPager = (SidebarAwareViewPager) findViewById(R.id.pager);
		this.pageSelectionListener = new VisiblePageSelectionListener(columnWidth);
		final MultiplexingOnPageChangeListener onPageChangeListener = new MultiplexingOnPageChangeListener(
				this.pageSelectionListener,
				new NotificationClearingPageSelectionListener(this));
		this.viewPager.setOnPageChangeListener(onPageChangeListener);
		this.viewPager.setAdapter(sectionsPagerAdapter);
		if (!showPageFromIntent(getIntent())) {
			if (this.columnsRtl) {
				gotoPage(0);
			}
			else {
				onPageChangeListener.onPageSelected(this.viewPager.getCurrentItem());
			}
		}

		final ActionBar ab = getActionBar();
		ab.setDisplayShowHomeEnabled(true);
		ab.setHomeButtonEnabled(true);
		ab.setDisplayShowTitleEnabled(false);
		ab.setDisplayShowCustomEnabled(true);

		this.columnTitleStrip = new ColumnTitleStrip(ab.getThemedContext());
		this.columnTitleStrip.setViewPager(this.viewPager);
		this.columnTitleStrip.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		this.columnTitleStrip.setColumnClickListener(new TitleClickListener(this));
		ab.setCustomView(this.columnTitleStrip);

		AlarmReceiver.configureAlarms(this);
		new CheckBackgroundUpdatesRunning(this, this.executorStatus).executeOnExecutor(this.localEs);
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
			finish();
			startActivity(getIntent());
			return;
		}

		startBgFetchVisibleColumnsIfConfigured();
	}

	@Override
	protected void onDestroy () {
		stopBgFetchVisibleColumns();
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

	@Override
	public DbInterface getDb () {
		final DbClient d = this.bndDb;
		if (d == null) return null;
		return d.getDb();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	Prefs getPrefs () {
		return this.prefs;
	}

	boolean isShowFiltered () {
		return this.prefs.getSharedPreferences().getBoolean(FiltersPrefFragment.KEY_SHOW_FILTERED, false);
	}

	Config getConf () {
		return this.conf;
	}

	public ExecutorEventListener getExecutorEventListener () {
		return this.executorStatus;
	}

	public Executor getLocalEs () {
		return this.localEs;
	}

	public Executor getNetEs () {
		return this.netEs;
	}

	@Override
	public ProviderMgr getProviderMgr () {
		if (!waitForDbReady()) throw new IllegalStateException("DB not bound.");
		return this.providerMgr;
	}

	void setProviderMgr (final ProviderMgr providerMgr) {
		this.providerMgr = providerMgr;
	}

	public HybridBitmapCache getImageCache () {
		return this.imageCache;
	}

	@Override
	public void loadImage (final ImageLoadRequest req) {
		ImageLoaderUtils.loadImage(this.imageCache, req, this.localEs, this.netEs, this.executorStatus);
	}

	public int convertPagePosition (final int argPosition) {
		if (!this.columnsRtl) return argPosition;
		final int count = this.viewPager.getAdapter().getCount();
		if (count < 1) return argPosition;
		return count - 1 - argPosition;
	}

	public boolean gotoPage (final int argPosition) {
		final int position = convertPagePosition(argPosition);
		if (this.viewPager.getCurrentItem() != position) {
			this.viewPager.setCurrentItem(position, false);
			return true;
		}
		return false;
	}

	public void gotoTweet (final int colId, final Tweet tweet) {
		gotoPage(getConf().getColumnPositionById(colId));
		final TweetListFragment page = getActivePageById(colId);
		if (page != null) page.scrollToTweet(tweet);
	}

	private boolean showPageFromIntent (final Intent intent) {
		if (intent.hasExtra(ARG_FOCUS_COLUMN_ID)) {
			final int pos = this.conf.getColumnPositionById(intent.getIntExtra(ARG_FOCUS_COLUMN_ID, 0));
			if (pos >= 0) return gotoPage(pos);
		}
		return false;
	}

	protected TweetListFragment getActivePageById (final int colId) {
		return this.activePages.get(colId);
	}

	protected void onFragmentResumed (final int columnId, final TweetListFragment page) {
		this.activePages.put(columnId, page);
	}

	protected void onFragmentPaused (final int columnId) {
		this.activePages.remove(columnId);
	}

	protected ScrollState getColumnScroll (final int columnId) {
		final TweetListFragment page = getActivePageById(columnId);
		if (page == null) return null;
		return page.getCurrentScroll();
	}

	@Override
	public void onBackPressed () {
		for (int i = 0; i < this.activePages.size(); i++) {
			final TweetListFragment page = this.activePages.valueAt(i);
			if (this.pageSelectionListener.isVisible(convertPagePosition(page.getColumnPosition())) && page.getSidebar().closeSidebar()) return;
		}
		super.onBackPressed();
	}

	public List<Column> getVisibleColumns () {
		final List<Column> ret = new ArrayList<Column>();
		for (int i = 0; i < this.conf.getColumnCount(); i++) {
			if (this.pageSelectionListener.isVisible(convertPagePosition(i))) ret.add(this.conf.getColumnByPosition(i));
		}
		return ret;
	}

	public int[] getVisibleColumnIds () {
		final List<Column> pages = getVisibleColumns();
		final int[] ret = new int[pages.size()];
		for (int i = 0; i < pages.size(); i++) {
			ret[i] = pages.get(i).getId();
		}
		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public boolean onCreateOptionsMenu (final Menu menu) {
		getMenuInflater().inflate(R.menu.listmenu, menu);

		/* FIXME apparently this.pageSelectionListener can sometimes be null here.
		 * Reported on Galaxy S5 stock firmware v6.0.1.
		 */
		if (this.pageSelectionListener != null && this.pageSelectionListener.getVisiblePageCount() > 1) {
			menu.findItem(R.id.mnuRefreshColumnNow).setTitle(R.string.main_menu_refresh_visible_columns);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected (final MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				new GotoMenu(this).onClick(this.columnTitleStrip); // TODO FIXME position it correctly under icon.
				return true;
			case R.id.mnuPost:
				showPost();
				return true;
			case R.id.mnuOutbox:
				showOutbox();
				return true;
			case R.id.mnuRefreshColumnNow:
				scheduleRefreshInteractive(getVisibleColumnIds());
				return true;
			case R.id.mnuRefreshAllNow:
				scheduleRefreshInteractive();
				return true;
			case R.id.mnuPreferences:
				startActivity(new Intent(this, OsPreferenceActivity.class));
				return true;
			case R.id.mnuLocalSearch:
				showLocalSearch();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void showPost () {
		showPost(getVisibleColumns(), null, -1);
	}

	protected void showPost (final Collection<Column> cols, final String initialBody, final int cursorPosition) {
		final Set<String> acIds = Column.uniqAccountIds(cols);
		final String accountId = acIds.size() == 1 ? acIds.iterator().next() : null;
		showPost(this.conf.getAccount(accountId), initialBody, cursorPosition);
	}

	private void showPost (final Account account, final String initialBody, final int cursorPosition) {
		final Intent intent = new Intent(this, PostActivity.class);
		if (account != null) intent.putExtra(PostActivity.ARG_ACCOUNT_ID, account.getId());
		if (initialBody != null) intent.putExtra(PostActivity.ARG_BODY, initialBody);
		if (cursorPosition >= 0) intent.putExtra(PostActivity.ARG_BODY_CURSOR_POSITION, cursorPosition);
		startActivity(intent);
	}

	private void showOutbox () {
		startActivity(new Intent(this, OutboxActivity.class));
	}

	/**
	 * return true if scheduled.
	 */
	protected boolean scheduleRefreshInteractive (final int... columnIds) {
		if (!NetHelper.connectionPresent(this)) {
			DialogHelper.alert(this, getString(R.string.main_no_internet_connection_available));
			return false;
		}
		scheduleRefresh(true, columnIds);
		return true;
	}

	protected void scheduleRefreshBackground (final int... columnIds) {
		if (!NetHelper.connectionPresent(this)) {
			Toast.makeText(this, R.string.main_no_internet_connection_available, Toast.LENGTH_SHORT).show();
			return;
		}
		scheduleRefresh(false, columnIds);
	}

	private void scheduleRefresh (final boolean manual, final int... columnIds) {
		final Intent intent = new Intent(this, UpdateService.class);
		if (manual) intent.putExtra(UpdateService.ARG_IS_MANUAL, true);
		if (columnIds != null && columnIds.length > 0) intent.putExtra(UpdateService.ARG_COLUMN_IDS, columnIds);
		startService(intent);

		if (manual) {
			final int count = columnIds == null ? 0 : columnIds.length;
			final String msg = count > 0
					? String.format(getString(R.string.main_refresh_columns_requested), count)
					: getString(R.string.main_refresh_all_columns_requested);
			Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
		}
	}

	private void showLocalSearch () {
		LocalSearchDialog.show(this, getConf(), this, this, new OnTweetListener() {
			@Override
			public void onTweet (final int colId, final Tweet tweet) {
				MainActivity.this.gotoTweet(colId, tweet);
			}
		});
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected void setTempColumnTitle (final int argPosition, final String title) {
		final int position = convertPagePosition(argPosition);
		this.columnTitleStrip.setTempColumnTitle(position, title);
	}

	private int progressIndicatorCounter = 0;

	/**
	 * Only call on UI thread.
	 */
	protected void progressIndicator (final boolean inProgress) {
		this.progressIndicatorCounter += (inProgress ? 1 : -1);
		setProgressBarIndeterminateVisibility(this.progressIndicatorCounter > 0);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private enum Msgs {
		BG_FETCH_VISIBLE_COLUMNS;
		public static final Msgs values[] = values(); // Optimisation to avoid new array every time.
	}

	private static class MessageHandler extends Handler {

		private final WeakReference<MainActivity> parentRef;

		public MessageHandler (final MainActivity parent) {
			this.parentRef = new WeakReference<MainActivity>(parent);
		}

		@Override
		public void handleMessage (final Message msg) {
			final MainActivity parent = this.parentRef.get();
			if (parent != null) parent.msgOnUiThread(msg);
		}
	}

	protected void msgOnUiThread (final Message msg) {
		switch (Msgs.values[msg.what]) {
			case BG_FETCH_VISIBLE_COLUMNS:
				bgFetchVisibleColumns();
				break;
			default:
		}
	}

	private boolean frequentlyFetchVisibleColumns = false;

	private void startBgFetchVisibleColumnsIfConfigured () {
		if (this.frequentlyFetchVisibleColumns) return;
		this.frequentlyFetchVisibleColumns = isAnyFrequentFetchColumnsConfigured();
		if (this.frequentlyFetchVisibleColumns) {
			this.msgHandler.sendEmptyMessageDelayed(Msgs.BG_FETCH_VISIBLE_COLUMNS.ordinal(), TimeUnit.SECONDS.toMillis(C.FETCH_VISIBLE_INITIAL_SECONDS));
			LOG.i("Frequently fetch visible colunns enabled.");
		}
	}

	private void stopBgFetchVisibleColumns () {
		this.frequentlyFetchVisibleColumns = false;
	}

	private void bgFetchVisibleColumns () {
		if (!this.frequentlyFetchVisibleColumns) return;
		final int[] visibleColumnIds = getVisibleColumnIds();
		if (visibleColumnIds.length > 0) {
			scheduleRefreshBackground(visibleColumnIds);
			LOG.i("Requested fetch of visible colunns: %s.", Arrays.toString(visibleColumnIds));
		}
		else {
			LOG.i("No visible colunns to refresh refresh of: %s.", Arrays.toString(visibleColumnIds));
		}
		this.msgHandler.sendEmptyMessageDelayed(Msgs.BG_FETCH_VISIBLE_COLUMNS.ordinal(), TimeUnit.MINUTES.toMillis(C.FETCH_VISIBLE_INTERVAL_MIN));
	}

	private boolean isAnyFrequentFetchColumnsConfigured () {
		for (final Column col : this.conf.getColumns()) {
			if (col.getRefreshIntervalMins() > 0 && col.getRefreshIntervalMins() < C.FETCH_VISIBLE_THRESHOLD_MIN) return true;
		}
		return false;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static class SectionsPagerAdapter extends FragmentPagerAdapter {

		private final MainActivity host;
		private final float pageWidth;

		public SectionsPagerAdapter (final FragmentManager fm, final MainActivity host, final float pageWidth) {
			super(fm);
			this.host = host;
			this.pageWidth = pageWidth;
		}

		@Override
		public Fragment getItem (final int argPosition) {
			final int position = this.host.convertPagePosition(argPosition);
			final Column col = this.host.getConf().getColumnByPosition(position);
			final Fragment fragment = new TweetListFragment();
			final Bundle args = new Bundle();
			args.putInt(TweetListFragment.ARG_COLUMN_POSITION, position);
			args.putInt(TweetListFragment.ARG_COLUMN_ID, col.getId());
			args.putString(TweetListFragment.ARG_COLUMN_TITLE, col.getTitle());
			args.putBoolean(TweetListFragment.ARG_COLUMN_IS_LATER, InternalColumnType.LATER.matchesColumn(col));
			args.putString(TweetListFragment.ARG_COLUMN_SHOW_INLINEMEDIA, col.getInlineMediaStyle() != null ? col.getInlineMediaStyle().serialise() : null);
			args.putBoolean(TweetListFragment.ARG_SHOW_FILTERED, this.host.isShowFiltered());
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount () {
			return this.host.getConf().getColumns().size();
		}

		@Override
		public CharSequence getPageTitle (final int argPosition) {
			final int position = this.host.convertPagePosition(argPosition);
			return this.host.getConf().getColumnByPosition(position).getTitle();
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

		public int getVisiblePageCount () {
			return this.visiblePages;
		}

		public boolean isVisible (final int position) {
			return position >= this.selectedPagePosition && position < this.selectedPagePosition + this.visiblePages;
		}

	}

	private static class TitleClickListener implements ColumnClickListener {

		private final MainActivity host;

		public TitleClickListener (final MainActivity host) {
			this.host = host;
		}

		@Override
		public void onColumnTitleClick (final int argPosition) {
			final int position = this.host.convertPagePosition(argPosition);
			final Column col = this.host.getConf().getColumnByPosition(position);
			if (col == null) return;
			final TweetListFragment page = this.host.getActivePageById(col.getId());
			if (page == null) return;
			page.scrollJumpUp();
		}

	}

	private static class NotificationClearingPageSelectionListener extends SimpleOnPageChangeListener {

		private final MainActivity host;

		public NotificationClearingPageSelectionListener (final MainActivity host) {
			this.host = host;
		}

		@Override
		public void onPageSelected (final int argPosition) {
			final int position = this.host.convertPagePosition(argPosition);
			final Column col = this.host.getConf().getColumnByPosition(position);
			Notifications.clearColumn(this.host, col);
		}

	}

	private static class CheckBackgroundUpdatesRunning extends TrackingAsyncTask<Void, Void, Boolean> {

		private final Activity activity;

		public CheckBackgroundUpdatesRunning (final Activity activity, final ExecutorEventListener eventListener) {
			super(eventListener);
			this.activity = activity;
		}

		@Override
		protected Boolean doInBackgroundWithTracking (final Void... params) {
			final float bl = BatteryHelper.level(this.activity.getApplicationContext());
			return bl <= C.MIN_BAT_UPDATE
					&& !BatteryNotify.isOverrideEnabled(this.activity);
		}

		@Override
		protected void onPostExecute (final Boolean backgroudUpdatesDisabled) {
			if (backgroudUpdatesDisabled) {
				final View backgroundUpdateDetails = this.activity.findViewById(R.id.backgroudUpdateDetails);
				backgroundUpdateDetails.setVisibility(View.VISIBLE);
				this.activity.findViewById(R.id.backgroundUpdateOverride).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick (final View v) {
						BatteryNotify.enableOverride(CheckBackgroundUpdatesRunning.this.activity);
						backgroundUpdateDetails.setVisibility(View.GONE);
					}
				});
			}
		}

	}

}
