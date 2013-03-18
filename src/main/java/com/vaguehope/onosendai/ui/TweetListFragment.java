package com.vaguehope.onosendai.ui;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
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
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.InternalColumnType;
import com.vaguehope.onosendai.layouts.SidebarLayout;
import com.vaguehope.onosendai.layouts.SidebarLayout.SidebarListener;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.model.TweetListAdapter;
import com.vaguehope.onosendai.payload.PayloadListAdapter;
import com.vaguehope.onosendai.payload.PayloadUtils;
import com.vaguehope.onosendai.storage.DbClient;
import com.vaguehope.onosendai.update.UpdateService;
import com.vaguehope.onosendai.util.ListViewHelper;
import com.vaguehope.onosendai.util.LogWrapper;

/**
 * https://developer.android.com/intl/fr/guide/components/fragments.html#
 * Creating
 */
public class TweetListFragment extends Fragment {

	static final String ARG_COLUMN_ID = "column_id";
	static final String ARG_COLUMN_TITLE = "column_title";
	static final String ARG_COLUMN_IS_LATER = "column_is_later";

	protected final LogWrapper log = new LogWrapper();
	private final DateFormat dateFormat = DateFormat.getDateTimeInstance();

	private int columnId;
	private boolean isLaterColumn;
	private RefreshUiHandler refreshUiHandler;

	private SidebarLayout sidebar;
	private ListView tweetList;

	private TextView txtTweetBody;
	private TextView txtTweetName;
	private TextView txtTweetDate;
	private PayloadListAdapter lstTweetPayloadAdaptor;
	private Button btnDetailsLater;

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
		this.isLaterColumn = getArguments().getBoolean(ARG_COLUMN_IS_LATER, false);
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

		Button btnColumnTitle = (Button) rootView.findViewById(R.id.tweetListTitle);
		btnColumnTitle.setText(getArguments().getString(ARG_COLUMN_TITLE));
		btnColumnTitle.setOnClickListener(this.columnTitleClickListener);

		Button btnRefresh = (Button) rootView.findViewById(R.id.tweetListRefresh);
		if (this.isLaterColumn) {
			((ViewGroup) btnRefresh.getParent()).removeView(btnRefresh);
		}
		else {
			btnRefresh.setOnClickListener(this.refreshClickListener);
		}

		this.tweetList = (ListView) rootView.findViewById(R.id.tweetListList);
		this.adapter = new TweetListAdapter(container.getContext());
		this.tweetList.setAdapter(this.adapter);
		this.tweetList.setScrollbarFadingEnabled(false);
		this.tweetList.setOnItemClickListener(this.tweetItemClickedListener);
		this.refreshUiHandler = new RefreshUiHandler(this);

		this.txtTweetBody = (TextView) rootView.findViewById(R.id.tweetDetailBody);
		this.txtTweetName = (TextView) rootView.findViewById(R.id.tweetDetailName);
		this.txtTweetDate = (TextView) rootView.findViewById(R.id.tweetDetailDate);
		this.lstTweetPayloadAdaptor = new PayloadListAdapter(container.getContext());
		((ListView) rootView.findViewById(R.id.tweetDetailPayloadList)).setAdapter(this.lstTweetPayloadAdaptor);
		((Button) rootView.findViewById(R.id.tweetDetailClose)).setOnClickListener(new SidebarLayout.ToggleSidebarListener(this.sidebar));
		this.btnDetailsLater = (Button) rootView.findViewById(R.id.tweetDetailLater);

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

	protected void scrollTop () {
		this.tweetList.setSelectionAfterHeaderView();
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

	protected DbClient getBndDb () {
		return this.bndDb;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected void scheduleRefresh () {
		getActivity().startService(new Intent(getActivity(), UpdateService.class));
		Toast.makeText(getActivity(), "Refresh all columns requested.", Toast.LENGTH_SHORT).show();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final OnClickListener columnTitleClickListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			scrollTop();
		}
	};

	private final OnClickListener refreshClickListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			scheduleRefresh();
		}
	};


	private final OnItemClickListener tweetItemClickedListener = new OnItemClickListener() {
		@Override
		public void onItemClick (final AdapterView<?> parent, final View view, final int position, final long id) {
			showTweetDetails(TweetListFragment.this.adapter.getInputData().getTweet(position));
		}
	};

	protected void showTweetDetails (final Tweet tweet) {
		this.txtTweetBody.setText(tweet.getBody());
		this.txtTweetName.setText(tweet.getUsername());
		this.txtTweetDate.setText(this.dateFormat.format(new Date(tweet.getTime() * 1000L)));
		this.lstTweetPayloadAdaptor.setInputData(PayloadUtils.extractPayload(tweet));
		setReadLaterButton(tweet, this.isLaterColumn);
		this.sidebar.openSidebar();
	}

	protected void setReadLaterButton (final Tweet tweet, final boolean laterColumn) {
		this.btnDetailsLater.setText(laterColumn ? R.string.btn_tweet_read : R.string.btn_tweet_later);
		this.btnDetailsLater.setOnClickListener(new DetailsLaterClickListener(this, tweet, laterColumn));
	}

	private static class DetailsLaterClickListener implements OnClickListener {

		private final TweetListFragment tweetListFragment;
		private final Context context;
		private final Tweet tweet;
		private final boolean isLaterColumn;

		public DetailsLaterClickListener (final TweetListFragment tweetListFragment, final Tweet tweet, final boolean isLaterColumn) {
			this.tweetListFragment = tweetListFragment;
			this.context = tweetListFragment.getActivity();
			this.tweet = tweet;
			this.isLaterColumn = isLaterColumn;
		}

		@Override
		public void onClick (final View v) {
			try {
				Config conf = new Config();
				Column col = conf.findInternalColumn(InternalColumnType.LATER);
				if (col != null) {
					if (this.isLaterColumn) {
						this.tweetListFragment.getBndDb().getDb().deleteTweet(col, this.tweet);
						Toast.makeText(this.context, "Removed.", Toast.LENGTH_SHORT).show();
					}
					else {
						this.tweetListFragment.getBndDb().getDb().storeTweets(col, Collections.singletonList(this.tweet));
						Toast.makeText(this.context, "Saved for later.", Toast.LENGTH_SHORT).show();
					}
					this.tweetListFragment.setReadLaterButton(this.tweet, !this.isLaterColumn);
				}
				else {
					Toast.makeText(this.context, "Read later column not configured.", Toast.LENGTH_SHORT).show();
				}
			}
			catch (IOException e) {
				Toast.makeText(this.context, e.toString(), Toast.LENGTH_LONG).show();
			}
			catch (JSONException e) {
				Toast.makeText(this.context, e.toString(), Toast.LENGTH_LONG).show();
			}
		}
	}

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

	protected Runnable getGuiUpdateRunnable () {
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
