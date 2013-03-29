package com.vaguehope.onosendai.ui;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.InternalColumnType;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.images.ImageLoaderUtils;
import com.vaguehope.onosendai.layouts.SidebarLayout;
import com.vaguehope.onosendai.layouts.SidebarLayout.SidebarListener;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.model.TweetListAdapter;
import com.vaguehope.onosendai.payload.InReplyToLoaderTask;
import com.vaguehope.onosendai.payload.InReplyToPayload;
import com.vaguehope.onosendai.payload.Payload;
import com.vaguehope.onosendai.payload.PayloadClickListener;
import com.vaguehope.onosendai.payload.PayloadListAdapter;
import com.vaguehope.onosendai.payload.PayloadListClickListener;
import com.vaguehope.onosendai.payload.PayloadType;
import com.vaguehope.onosendai.payload.PayloadUtils;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.provider.RtTask;
import com.vaguehope.onosendai.provider.RtTask.RtRequest;
import com.vaguehope.onosendai.storage.DbClient;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.storage.DbInterface.TwUpdateListener;
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

	private int columnId = -1;
	private boolean isLaterColumn;
	private Config conf;
	private ProviderMgr providerMgr;
	private ImageLoader imageLoader;
	private RefreshUiHandler refreshUiHandler;

	private SidebarLayout sidebar;
	private ListView tweetList;

	private TextView txtTweetBody;
	private ImageView imgTweetAvatar;
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

		final MainActivity mainActivity = (MainActivity) getActivity();
		this.conf = mainActivity.getConf();
		this.providerMgr = mainActivity.getProviderMgr();
		this.imageLoader = ImageLoaderUtils.fromActivity(getActivity());

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

		Button btnMenu = (Button) rootView.findViewById(R.id.tweetListMenu);
		if (this.isLaterColumn) {
			((ViewGroup) btnMenu.getParent()).removeView(btnMenu);
		}
		else {
			btnMenu.setOnClickListener(this.menuClickListener);
		}

		this.tweetList = (ListView) rootView.findViewById(R.id.tweetListList);
		this.adapter = new TweetListAdapter(container.getContext(), this.imageLoader);
		this.tweetList.setAdapter(this.adapter);
		this.tweetList.setScrollbarFadingEnabled(false);
		this.tweetList.setOnItemClickListener(this.tweetItemClickedListener);
		this.refreshUiHandler = new RefreshUiHandler(this);

		ListView lstTweetPayload = (ListView) rootView.findViewById(R.id.tweetDetailPayloadList);
		lstTweetPayload.addHeaderView(inflater.inflate(R.layout.tweetdetail, null));
		this.lstTweetPayloadAdaptor = new PayloadListAdapter(container.getContext(), this.imageLoader, this.payloadClickListener);
		lstTweetPayload.setAdapter(this.lstTweetPayloadAdaptor);
		lstTweetPayload.setOnItemClickListener(new PayloadListClickListener(this.payloadClickListener));
		this.txtTweetBody = (TextView) rootView.findViewById(R.id.tweetDetailBody);
		this.imgTweetAvatar = (ImageView) rootView.findViewById(R.id.tweetDetailAvatar);
		this.txtTweetName = (TextView) rootView.findViewById(R.id.tweetDetailName);
		this.txtTweetDate = (TextView) rootView.findViewById(R.id.tweetDetailDate);
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

	public int getColumnId () {
		return this.columnId;
	}

	Config getConf () {
		return this.conf;
	}

	private Account getColumnAccount () {
		final Column column = this.conf.getColumnById(this.columnId);
		return this.conf.getAccount(column.getAccountId());
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
		getDb().storeScroll(this.columnId, this.scrollState);
	}

	protected void restoreSavedScrollFromDb () {
		if (this.scrollState != null) return;
		this.scrollState = getDb().getScroll(this.columnId);
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
					getDb().addTwUpdateListener(getGuiUpdateListener());
					restoreSavedScrollFromDb();
					refreshUi();
					TweetListFragment.this.log.d("DB service bound.");
				}
			});
		}
		else { // because we stop listening in onPause(), we must resume if the user comes back.
			this.bndDb.getDb().addTwUpdateListener(getGuiUpdateListener());
			restoreSavedScrollFromDb();
			refreshUi();
			this.log.d("DB service rebound.");
		}
	}

	private void suspendDb () {
		// We might be pausing before the callback has come.
		if (this.bndDb.getDb() != null) {
			this.bndDb.getDb().removeTwUpdateListener(getGuiUpdateListener());
		}
		else {
			// If we have not even had the callback yet, cancel it.
			this.bndDb.clearReadyListener();
		}
		this.log.d("DB service released.");
	}

	protected DbInterface getDb () {
		final DbClient d = this.bndDb;
		if (d == null) return null;
		return d.getDb();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected void scheduleRefresh (final boolean all) {
		final Intent intent = new Intent(getActivity(), UpdateService.class);
		intent.putExtra(UpdateService.ARG_IS_MANUAL, true);
		if (!all) intent.putExtra(UpdateService.ARG_COLUMN_ID, this.columnId);
		getActivity().startService(intent);

		final String msg = all ? "Refresh all columns requested." : "Refresh column requested.";
		Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final OnClickListener columnTitleClickListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			scrollTop();
		}
	};

	private final OnClickListener menuClickListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			PopupMenu popupMenu = new PopupMenu(getActivity(), v);
			popupMenu.getMenuInflater().inflate(R.menu.listmenu, popupMenu.getMenu());
			popupMenu.setOnMenuItemClickListener(TweetListFragment.this.menuItemClientListener);
			popupMenu.show();
		}
	};

	protected final PopupMenu.OnMenuItemClickListener menuItemClientListener = new PopupMenu.OnMenuItemClickListener() {
		@Override
		public boolean onMenuItemClick (final MenuItem item) {
			switch (item.getItemId()) {
				case R.id.mnuPost:
					showPost();
					return true;
				case R.id.mnuRefreshColumnNow:
					scheduleRefresh(false);
					return true;
				case R.id.mnuRefreshAllNow:
					scheduleRefresh(true);
					return true;
				default:
					return false;
			}
		}
	};

	private final OnItemClickListener tweetItemClickedListener = new OnItemClickListener() {
		@Override
		public void onItemClick (final AdapterView<?> parent, final View view, final int position, final long id) {
			showTweetDetails(TweetListFragment.this.adapter.getInputData().getTweet(position));
		}
	};

	private final PayloadClickListener payloadClickListener = new PayloadClickListener() {

		@Override
		public boolean payloadClicked (final Payload payload) {
			if (payload.getType() == PayloadType.INREPLYTO) {
				showTweetDetails(((InReplyToPayload) payload).getInReplyToTweet());
				return true;
			}
			return false;
		}

		@Override
		public void subviewClicked (final Payload payload, final int index) {
			if (payload.getType() == PayloadType.SHARE) {
				switch (index) {
					case 0:
						askRt(payload.getOwnerTweet());
						break;
					case 1:
						showPost(payload.getOwnerTweet());
						break;
					default:
				}
			}
		}

	};

	protected void showTweetDetails (final Tweet listTweet) {
		final Tweet dbTweet = getDb().getTweetDetails(this.columnId, listTweet);
		final Tweet tweet = dbTweet != null ? dbTweet : listTweet;
		this.txtTweetBody.setText(tweet.getBody());
		if (tweet.getAvatarUrl() != null) this.imageLoader.loadImage(new ImageLoadRequest(tweet.getAvatarUrl(), this.imgTweetAvatar));
		this.txtTweetName.setText(tweet.getFullname());
		this.txtTweetDate.setText(this.dateFormat.format(new Date(TimeUnit.SECONDS.toMillis(tweet.getTime()))));
		this.lstTweetPayloadAdaptor.setInputData(PayloadUtils.extractPayload(this.columnId, tweet));
		lookForInReplyTos(tweet);
		setReadLaterButton(tweet, this.isLaterColumn);
		this.sidebar.openSidebar();
	}

	public void lookForInReplyTos (final Tweet tweet) {
		final Column column = this.conf.getColumnById(this.columnId);
		final Account account = this.conf.getAccount(column.getAccountId());
		new InReplyToLoaderTask(account, column, this.providerMgr, getDb(), this.lstTweetPayloadAdaptor).execute(tweet);
	}

	protected void setReadLaterButton (final Tweet tweet, final boolean laterColumn) {
		this.btnDetailsLater.setText(laterColumn ? R.string.btn_tweet_read : R.string.btn_tweet_later);
		this.btnDetailsLater.setOnClickListener(new DetailsLaterClickListener(this, tweet, laterColumn));
	}

	protected void showPost () {
		showPost(null);
	}

	protected void showPost (final Tweet tweetToQuote) {
		final Intent intent = new Intent(getActivity(), PostActivity.class)
				.putExtra(PostActivity.ARG_COLUMN_ID, this.columnId);
		if (tweetToQuote != null) {
			intent.putExtra(PostActivity.ARG_BODY,
					String.format("RT @%s %s", tweetToQuote.getUsername(), tweetToQuote.getBody()));
		}
		startActivity(intent);
	}

	protected void askRt (final Tweet tweet) {
		final AlertDialog.Builder dlgBld = new AlertDialog.Builder(getActivity());
		dlgBld.setMessage(String.format("RT @%s ?", tweet.getUsername()));

		dlgBld.setPositiveButton("RT", new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				doRt(tweet);
			}
		});

		dlgBld.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int whichButton) {
				dialog.cancel();
			}
		});

		dlgBld.show();
	}

	protected void doRt(final Tweet tweet) {
		new RtTask(getActivity(), new RtRequest(getColumnAccount(), tweet)).execute();
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
			Column col = this.tweetListFragment.getConf().findInternalColumn(InternalColumnType.LATER);
			if (col != null) {
				if (this.isLaterColumn) {
					this.tweetListFragment.getDb().deleteTweet(col, this.tweet);
					Toast.makeText(this.context, "Removed.", Toast.LENGTH_SHORT).show();
				}
				else {
					this.tweetListFragment.getDb().storeTweets(col, Collections.singletonList(this.tweet));
					Toast.makeText(this.context, "Saved for later.", Toast.LENGTH_SHORT).show();
				}
				this.tweetListFragment.setReadLaterButton(this.tweet, !this.isLaterColumn);
			}
			else {
				Toast.makeText(this.context, "Read later column not configured.", Toast.LENGTH_SHORT).show();
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

	protected TwUpdateListener getGuiUpdateListener () {
		return this.guiUpdateListener;
	}

	private final TwUpdateListener guiUpdateListener = new TwUpdateListener() {
		@Override
		public void columnChanged (final int changeColumnId) {
			if (changeColumnId != getColumnId()) return;
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
		final DbInterface db = getDb();
		if (db != null) {
			List<Tweet> tweets = db.getTweets(this.columnId, 200); // FIXME replace 200 with dynamic list.
			saveScrollIfNotSaved();
			this.adapter.setInputData(new TweetList(tweets));
			this.log.d("Refreshed %d tweets.", tweets.size());
			restoreScroll();
		}
		else {
			this.log.w("Failed to refresh column as DB was not bound.");
		}
	}

}
