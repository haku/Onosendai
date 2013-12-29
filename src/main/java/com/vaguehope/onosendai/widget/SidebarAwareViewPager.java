package com.vaguehope.onosendai.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

public class SidebarAwareViewPager extends ViewPager {

	public SidebarAwareViewPager (final Context context) {
		super(context);
	}

	public SidebarAwareViewPager (final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected boolean canScroll (final View v, final boolean checkV, final int dx, final int x, final int y) {
		return super.canScroll(v, checkV, dx, x, y) || (checkV && customCanScroll(v));
	}

	protected boolean customCanScroll (final View v) {
		if (v instanceof SidebarLayout) {
			return ((SidebarLayout) v).isOpen();
		}
		return false;
	}

}
