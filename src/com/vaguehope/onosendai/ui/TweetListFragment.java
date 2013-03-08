package com.vaguehope.onosendai.ui;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.TweetListAdapter;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.storage.DbClient;

public class TweetListFragment extends Fragment {

	public static final String ARG_COLUMN_ID = "column_id";

	private int columnId;
	private TweetListAdapter adapter;
	private DbClient bndDb;

	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.columnId = getArguments().getInt(ARG_COLUMN_ID);
		ListView tweetList = new ListView(getActivity());
		this.adapter = new TweetListAdapter(getActivity());
		tweetList.setAdapter(this.adapter);
		return tweetList;
	}

	@Override
	public void onDestroy () {
		this.bndDb.finalize();
		super.onDestroy();
	}

	@Override
	public void onResume () {
		super.onResume();
		resumeDb();
	}

	@Override
	public void onPause () {
		super.onPause();
		suspendDb();
	}

	private void resumeDb () {
		Log.i(C.TAG, "UI Binding DB service...");
		if (this.bndDb == null) {
			this.bndDb = new DbClient(getActivity(), new Runnable() {
				@Override
				public void run () {
					/*
					 * this convoluted method is because the service
					 * connection won't finish until this thread processes
					 * messages again (i.e., after it exits this thread). if
					 * we try to talk to the DB service before then, it will
					 * NPE.
					 */
					getBndDb().getDb().addTwUpdateListener(getGuiUpdateRunnable());
					Log.i(C.TAG, "UI DB service bound.");
				}
			});
		}
		else { // because we stop listening in onPause(), we must resume if the user comes back.
			this.bndDb.getDb().addTwUpdateListener(getGuiUpdateRunnable());
			Log.i(C.TAG, "UI DB service rebound.");
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
		Log.i(C.TAG, "UI DB service released.");
	}

	public DbClient getBndDb () {
		return this.bndDb;
	}

	public Runnable getGuiUpdateRunnable () {
		return this.guiUpdateRunnable;
	}

	private Runnable guiUpdateRunnable = new Runnable() {
		@Override
		public void run () {
			refreshUi();
		}
	};

	protected void refreshUi () {
		ArrayList<Tweet> tweets = this.bndDb.getDb().getTweets(this.columnId, 200);
		this.adapter.setInputData(new TweetList(tweets));
	}

}