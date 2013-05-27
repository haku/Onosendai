package com.vaguehope.onosendai.widget;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

public final class ScrollIndicator {

	private static final int ACCEL_STOP_1 = 5;
	private static final int ACCEL_STOP_2 = 15;
	private static final int ACCEL_STOP_3 = 100;
	private static final int BAR_WIDTH_DIP = 7;
	private static final int COLOUR = Color.GRAY;

	private ScrollIndicator () {
		throw new AssertionError();
	}

	public static void attach (final Context context, final ViewGroup rootView, final ListView tweetList) {
		final AbsoluteShape bar = new AbsoluteShape(context);
		bar.layoutForce(0, 0, 0, 0);
		bar.setBackgroundColor(COLOUR);
		bar.bringToFront();
		rootView.addView(bar);

		final float pxPerUnit = dipToPixels(context, 1); // Perhaps this should be a % of list height?
		final int barWidth = (int) dipToPixels(context, BAR_WIDTH_DIP);

		tweetList.setOnScrollListener(new BarMovingScrollListener(bar, pxPerUnit, barWidth));
	}

	private static float dipToPixels (final Context context, final int a) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, a, context.getResources().getDisplayMetrics());
	}

	private static class AbsoluteShape extends View {

		public AbsoluteShape (final Context context) {
			super(context);
		}

		@Override
		public void layout (final int l, final int t, final int r, final int b) {
			/* block calls so no layout manager can not mess with us. */
		}

		public void layoutForce (final int l, final int t, final int r, final int b) {
			super.layout(l, t, r, b);
		}

	}

	private static class BarMovingScrollListener implements OnScrollListener {

		private final AbsoluteShape bar;
		private final float pxPerUnit;
		private final int barWidth;

		private int lastPosition = -1;

		public BarMovingScrollListener (final AbsoluteShape bar, final float pxPerUnit, final int barWidth) {
			this.bar = bar;
			this.pxPerUnit = pxPerUnit;
			this.barWidth = barWidth;
		}

		@Override
		public void onScrollStateChanged (final AbsListView view, final int scrollStateFlag) {
			final int position = view.getFirstVisiblePosition();
			if (position != this.lastPosition) {
				final int h = (int) ((position
						+ Math.min(position, ACCEL_STOP_1)
						+ Math.min(position, ACCEL_STOP_2)
						+ Math.min(position, ACCEL_STOP_3)) * this.pxPerUnit);
				this.bar.layoutForce(view.getRight() - this.barWidth, view.getTop(), view.getRight(), view.getTop() + h);
				this.lastPosition = position;
			}
		}

		@Override
		public void onScroll (final AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
			onScrollStateChanged(view, 0);
		}

	}

}
