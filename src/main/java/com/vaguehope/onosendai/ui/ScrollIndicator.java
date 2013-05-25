package com.vaguehope.onosendai.ui;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

public class ScrollIndicator {

	private static final int ITEM_HEIGHT_DIP = 2;
	private static final int BAR_WIDTH_DIP = 7;
	private static final int COLOUR = Color.GRAY;

	private ScrollIndicator () {
		throw new AssertionError();
	}

	public static void attach (final Context context, final ViewGroup rootView, final ListView tweetList) {
		final View bar = new View(context);
		bar.layout(0, 0, 0, 0);
		bar.setBackgroundColor(COLOUR);
		bar.bringToFront();
		rootView.addView(bar);

		final int barWidth = (int) dipToPixels(context, BAR_WIDTH_DIP);
		final float pxPerItem = dipToPixels(context, ITEM_HEIGHT_DIP);

		tweetList.setOnScrollListener(new BarMovingScrollListener(bar, pxPerItem, barWidth));
	}

	private static float dipToPixels (final Context context, final int a) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, a, context.getResources().getDisplayMetrics());
	}

	private static class BarMovingScrollListener implements OnScrollListener {

		private final View bar;
		private final float pxPerItem;
		private final int barWidth;

		public BarMovingScrollListener (final View bar, final float pxPerItem, final int barWidth) {
			this.bar = bar;
			this.pxPerItem = pxPerItem;
			this.barWidth = barWidth;
		}

		@Override
		public void onScrollStateChanged (final AbsListView view, final int scrollStateFlag) {
			final int y = (int) (view.getFirstVisiblePosition() * this.pxPerItem);
			this.bar.layout(view.getRight() - this.barWidth, view.getTop(), view.getRight(), view.getTop() + y);
		}

		@Override
		public void onScroll (final AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
			onScrollStateChanged(view, 0);
		}

	}

}
