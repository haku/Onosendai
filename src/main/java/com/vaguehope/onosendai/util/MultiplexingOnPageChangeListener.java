package com.vaguehope.onosendai.util;

import android.support.v4.view.ViewPager.OnPageChangeListener;

public class MultiplexingOnPageChangeListener implements OnPageChangeListener {

	private final OnPageChangeListener[] listeners;

	public MultiplexingOnPageChangeListener (final OnPageChangeListener... listeners) {
		this.listeners = listeners;
	}

	@Override
	public void onPageScrolled (final int position, final float positionOffset, final int positionOffsetPixels) {
		for (final OnPageChangeListener l : this.listeners) {
			l.onPageScrolled(position, positionOffset, positionOffsetPixels);
		}
	}

	@Override
	public void onPageSelected (final int position) {
		for (final OnPageChangeListener l : this.listeners) {
			l.onPageSelected(position);
		}
	}

	@Override
	public void onPageScrollStateChanged (final int state) {
		for (final OnPageChangeListener l : this.listeners) {
			l.onPageScrollStateChanged(state);
		}
	}

}
