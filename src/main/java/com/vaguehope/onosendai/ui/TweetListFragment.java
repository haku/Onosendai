package com.vaguehope.onosendai.ui;

import java.lang.ref.WeakReference;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.model.TweetListAdapter;
import com.vaguehope.onosendai.storage.DbClient;
import com.vaguehope.onosendai.util.ListViewHelper;
import com.vaguehope.onosendai.util.ListViewHelper.ScrollState;
import com.vaguehope.onosendai.util.LogWrapper;

public class TweetListFragment extends Fragment {

	public static final String ARG_COLUMN_ID = "column_id";

	protected final LogWrapper log = new LogWrapper();

	private int columnId;
	private RefreshUiHandler refreshUiHandler;
	private ListView listView;
	private ScrollState scrollState;
	private TweetListAdapter adapter;
	private DbClient bndDb;

	@Override
	public View onCreateView (final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		this.columnId = getArguments().getInt(ARG_COLUMN_ID);
		this.log.setPrefix("C" + this.columnId);

		this.refreshUiHandler = new RefreshUiHandler(this);
		this.listView = new ListView(getActivity());
		this.adapter = new TweetListAdapter(getActivity());
		this.listView.setAdapter(this.adapter);
		return this.listView;
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
		suspendDb();
		super.onPause();
	}

	@Override
	public void onSaveInstanceState (final Bundle outState) {
		super.onSaveInstanceState(outState);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void saveScroll () {
		this.scrollState = ListViewHelper.saveScrollState(this.listView);
		this.log.d("Saved scroll: " + this.scrollState);
	}

	private void restoreScroll () {
		if (this.scrollState == null) return;
		ListViewHelper.restoreScrollState(this.listView, this.scrollState);
		this.log.d("Restored scroll: " + this.scrollState);
		this.scrollState = null;
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
					refreshUi();
					TweetListFragment.this.log.d("DB service bound.");
				}
			});
		}
		else { // because we stop listening in onPause(), we must resume if the user comes back.
			this.bndDb.getDb().addTwUpdateListener(getGuiUpdateRunnable());
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
		this.adapter.setInputData(new TweetList(tweets));
		restoreScroll();
		this.log.i("Refreshed %d tweets.", tweets.size());
	}

}
