package com.vaguehope.onosendai;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.storage.DbClient;
import com.vaguehope.onosendai.update.AlarmReceiver;

public class MainActivity extends FragmentActivity {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	private SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	private ViewPager mViewPager;

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Create the adapter that will return a fragment for each of the three primary sections of the app.
		this.mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		this.mViewPager = (ViewPager) findViewById(R.id.pager);
		this.mViewPager.setAdapter(this.mSectionsPagerAdapter);

		AlarmReceiver.configureAlarm(this); // FIXME be more smart about this?
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public boolean onCreateOptionsMenu (Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public static class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter (FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem (int position) {
			// getItem is called to instantiate the fragment for the given page.
			Fragment fragment = new TweetListFragment();
			Bundle args = new Bundle();
			args.putInt(TweetListFragment.ARG_COLUMN_ID, position);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount () {
			return 3; // Show 3 total pages.
		}

		@Override
		public CharSequence getPageTitle (int position) {
			return "Column " + position;
		}

	}

	public static class TweetListFragment extends Fragment {

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

}
