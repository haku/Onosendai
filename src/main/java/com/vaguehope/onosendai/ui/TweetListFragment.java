package com.vaguehope.onosendai.ui;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.layouts.SidebarLayout;
import com.vaguehope.onosendai.layouts.SidebarLayout.SidebarListener;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.model.TweetListAdapter;
import com.vaguehope.onosendai.storage.DbClient;
import com.vaguehope.onosendai.util.ListViewHelper;
import com.vaguehope.onosendai.util.LogWrapper;

/**
 * https://developer.android.com/intl/fr/guide/components/fragments.html#
 * Creating
 */
public class TweetListFragment extends Fragment {

	public static final String ARG_COLUMN_ID = "column_id";

	protected final LogWrapper log = new LogWrapper();
	private final DateFormat dateFormat = DateFormat.getDateTimeInstance();

	private int columnId;
	private RefreshUiHandler refreshUiHandler;

	private SidebarLayout sidebar;
	private ListView tweetList;

	private TextView txtTweetBody;
	private TextView txtTweetName;
	private TextView txtTweetDate;

	private ScrollState scrollState;
	protected TweetListAdapter adapter;
	private DbClient bndDb;

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView (final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		this.columnId = getArguments().getInt(ARG_COLUMN_ID);
		this.log.setPrefix("C" + this.columnId);
		this.log.d("onCreateView()");

		/*
		 * Fragment life cycles are strange. onCreateView() is called multiple
		 * times before onSaveInstanceState() is called. Do not overwrite
		 * perfectly good stated stored in member var.
		 */
		if (this.scrollState == null) {
			this.scrollState = ListViewHelper.fromBundle(savedInstanceState);
		}

		final View rootView = inflater.inflate(R.layout.tweetlist, container, false);
		this.sidebar = (SidebarLayout) rootView.findViewById(R.id.tweetListLayout);
		this.sidebar.setListener(this.sidebarListener);

		rootView.setFocusableInTouchMode(true);
		rootView.requestFocus();
		rootView.setOnKeyListener(new SidebarLayout.BackButtonListener(this.sidebar));

		this.tweetList = (ListView) rootView.findViewById(R.id.tweetListList);
		this.adapter = new TweetListAdapter(container.getContext());
		this.tweetList.setAdapter(this.adapter);
		this.tweetList.setScrollbarFadingEnabled(false);
		this.tweetList.setOnItemClickListener(this.tweetItemClickedListener);
		this.refreshUiHandler = new RefreshUiHandler(this);

		this.txtTweetBody = (TextView) rootView.findViewById(R.id.tweetDetailBody);
		this.txtTweetName = (TextView) rootView.findViewById(R.id.tweetDetailName);
		this.txtTweetDate = (TextView) rootView.findViewById(R.id.tweetDetailDate);
		((Button) rootView.findViewById(R.id.tweetDetailClose)).setOnClickListener(new SidebarLayout.ToggleSidebarListener(this.sidebar));
		((Button) rootView.findViewById(R.id.tweetDetailLater)).setOnClickListener(this.laterClickListener);

		return rootView;
	}

	@Override
	public void onDestroy () {
		this.bndDb.dispose();
		super.onDestroy();
	}

	@Override
	public void onResume () {
		super.onResume();
		resumeDb();
	}

	@Override
	public void onPause () {
		saveScroll();
		saveSavedScrollToDb();
		suspendDb();
		super.onPause();
	}

	@Override
	public void onSaveInstanceState (final Bundle outState) {
		super.onSaveInstanceState(outState);
		ListViewHelper.toBundle(this.scrollState, outState);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void saveScroll () {
		ScrollState newState = ListViewHelper.saveScrollState(this.tweetList);
		if (newState != null) {
			this.scrollState = newState;
			this.log.d("Saved scroll: %s", this.scrollState);
		}
	}

	private void saveScrollIfNotSaved () {
		if (this.scrollState != null) return;
		saveScroll();
	}

	private void restoreScroll () {
		if (this.scrollState == null) return;
		ListViewHelper.restoreScrollState(this.tweetList, this.scrollState);
		this.log.d("Restored scroll: %s", this.scrollState);
		this.scrollState = null;
	}

	private void saveSavedScrollToDb () {
		this.bndDb.getDb().storeScroll(this.columnId, this.scrollState);
	}

	protected void restoreSavedScrollFromDb () {
		if (this.scrollState != null) return;
		this.scrollState = this.bndDb.getDb().getScroll(this.columnId);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void resumeDb () {
		this.log.d("Binding DB service...");
		if (this.bndDb == null) {
			this.bndDb = new DbClient(getActivity(), this.log.getPrefix(), new Runnable() {
				@Override
				public void run () {
					/*
					 * this convoluted method is because the service connection
					 * won't finish until this thread processes messages again
					 * (i.e., after it exits this thread). if we try to talk to
					 * the DB service before then, it will NPE.
					 */
					getBndDb().getDb().addTwUpdateListener(getGuiUpdateRunnable());
					restoreSavedScrollFromDb();
					refreshUi();
					TweetListFragment.this.log.d("DB service bound.");
				}
			});
		}
		else { // because we stop listening in onPause(), we must resume if the user comes back.
			this.bndDb.getDb().addTwUpdateListener(getGuiUpdateRunnable());
			restoreSavedScrollFromDb();
			refreshUi();
			this.log.d("DB service rebound.");
		}
	}

	private void suspendDb () {
		// We might be pausing before the callback has come.
		if (this.bndDb.getDb() != null) {
			this.bndDb.getDb().removeTwUpdateListener(getGuiUpdateRunnable());
		}
		else {
			// If we have not even had the callback yet, cancel it.
			this.bndDb.clearReadyListener();
		}
		this.log.d("DB service released.");
	}

	public DbClient getBndDb () {
		return this.bndDb;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	// https://developer.android.com/intl/fr/guide/topics/ui/actionbar.html#Home
	// http://www.grokkingandroid.com/adding-action-items-from-within-fragments/

	@Override
	public void onCreateOptionsMenu (final Menu menu, final MenuInflater inflater) {
		inflater.inflate(R.menu.tweetlist_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected (final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_jump_top:
				this.tweetList.setSelectionAfterHeaderView();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private final OnItemClickListener tweetItemClickedListener = new OnItemClickListener() {
		@Override
		public void onItemClick (final AdapterView<?> parent, final View view, final int position, final long id) {
			showTweet(TweetListFragment.this.adapter.getInputData().getTweet(position));
		}
	};

	protected void showTweet (final Tweet tweet) {
		this.txtTweetBody.setText(tweet.getBody());
		this.txtTweetName.setText(tweet.getUsername());
		this.txtTweetDate.setText(this.dateFormat.format(new Date(tweet.getTime() * 1000L)));
		this.sidebar.openSidebar();
	}

	private final OnClickListener laterClickListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			Toast.makeText(getActivity(), "TODO: save to read later.", Toast.LENGTH_SHORT).show();
		}
	};

	private final SidebarListener sidebarListener = new SidebarListener() {

		@Override
		public boolean onContentTouchedWhenOpening (final SidebarLayout sb) {
			sb.closeSidebar();
			return true;
		}

		@Override
		public void onSidebarOpened (final SidebarLayout sb) {/* Unused. */}

		@Override
		public void onSidebarClosed (final SidebarLayout sb) {/* Unused. */}
	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public Runnable getGuiUpdateRunnable () {
		return this.guiUpdateRunnable;
	}

	private final Runnable guiUpdateRunnable = new Runnable() {
		@Override
		public void run () {
			refreshUi();
		}
	};

	protected void refreshUi () {
		this.refreshUiHandler.sendEmptyMessage(0);
	}

	private static class RefreshUiHandler extends Handler {

		private final WeakReference<TweetListFragment> parentRef;

		public RefreshUiHandler (final TweetListFragment parent) {
			this.parentRef = new WeakReference<TweetListFragment>(parent);
		}

		@Override
		public void handleMessage (final Message msg) {
			TweetListFragment parent = this.parentRef.get();
			if (parent != null) parent.refreshUiOnUiThread();
		}

	}

	protected void refreshUiOnUiThread () {
		List<Tweet> tweets = this.bndDb.getDb().getTweets(this.columnId, 200);
		saveScrollIfNotSaved();
		this.adapter.setInputData(new TweetList(tweets));
		restoreScroll();
		this.log.d("Refreshed %d tweets.", tweets.size());
	}

}
