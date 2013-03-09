package com.vaguehope.onosendai.ui;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.widget.Toast;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Config;
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
	protected void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Config conf = null;
		try {
			conf = new Config();
		}
		catch (Exception e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		this.mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), conf);

		this.mViewPager = (ViewPager) findViewById(R.id.pager);
		this.mViewPager.setAdapter(this.mSectionsPagerAdapter);

		AlarmReceiver.configureAlarm(this); // FIXME be more smart about this?
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public boolean onCreateOptionsMenu (final Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public static class SectionsPagerAdapter extends FragmentPagerAdapter {

		private final Config conf;

		public SectionsPagerAdapter (final FragmentManager fm, final Config conf) {
			super(fm);
			this.conf = conf;
		}

		@Override
		public Fragment getItem (final int position) {
			// getItem is called to instantiate the fragment for the given page.
			Fragment fragment = new TweetListFragment();
			Bundle args = new Bundle();
			args.putInt(TweetListFragment.ARG_COLUMN_ID, position);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount () {
			return this.conf.getColumns().size();
		}

		@Override
		public CharSequence getPageTitle (final int position) {
			return this.conf.getColumn(position).title;
		}

	}

}
