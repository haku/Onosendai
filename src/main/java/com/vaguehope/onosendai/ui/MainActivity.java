package com.vaguehope.onosendai.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.Toast;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.InternalColumnType;
import com.vaguehope.onosendai.images.HybridBitmapCache;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.images.ImageLoaderUtils;
import com.vaguehope.onosendai.update.AlarmReceiver;

public class MainActivity extends FragmentActivity implements ImageLoader {

	private HybridBitmapCache imageCache;

	@Override
	protected void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Config conf = null;
		try {
			conf = new Config();
		}
		catch (Exception e) {
			Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		final float columnWidth = Float.parseFloat(getResources().getString(R.string.column_width));

		this.imageCache = new HybridBitmapCache(this, C.MAX_MEMORY_IMAGE_CACHE);

		// If this becomes too memory intensive, switch to android.support.v4.app.FragmentStatePagerAdapter.
		SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), conf, columnWidth);
		((ViewPager) findViewById(R.id.pager)).setAdapter(sectionsPagerAdapter);

		AlarmReceiver.configureAlarm(this); // FIXME be more smart about this?
	}

	@Override
	protected void onDestroy () {
		if (this.imageCache != null) this.imageCache.clean();
		super.onDestroy();
	}

	@Override
	public void loadImage (final ImageLoadRequest req) {
		ImageLoaderUtils.loadImage(this.imageCache, req);
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
			final Column col = this.conf.getColumns().get(position);
			// getItem is called to instantiate the fragment for the given page.
			Fragment fragment = new TweetListFragment();
			Bundle args = new Bundle();
			args.putInt(TweetListFragment.ARG_COLUMN_ID, col.id);
			args.putString(TweetListFragment.ARG_COLUMN_TITLE, col.title);
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
			return this.conf.getColumn(position).title;
		}

		@Override
		public float getPageWidth (final int position) {
			return this.pageWidth;
		}

	}

}
