package com.vaguehope.onosendai.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.model.TweetListCursorAdapter;

public final class ScrollIndicator {

	private static final double MAX_HEIGHT_RELATIVE = 0.95d;
	private static final double POSITION_MAX_HEIGHT = C.DATA_TW_MAX_COL_ENTRIES;
	private static final double ACCEL_BASE = 12d; // degree of non-linear effect.
	private static final double LOG_ACCEL_BASE = Math.log(ACCEL_BASE);
	/**
	 * Factored out for efficiency.  The JIT might do this, but might as well be sure.
	 */
	private static final double SCALE_CONSTANT_THING = 1d / (Math.exp(MAX_HEIGHT_RELATIVE * LOG_ACCEL_BASE) - 1);

	private static final int ACCEL_STOP_1 = 5;
	private static final int ACCEL_STOP_2 = 15;
	private static final int ACCEL_STOP_3 = 100;

	private static final int BAR_WIDTH_DIP = 7;
	private static final int COLOUR_BAR = Color.GRAY;
	private static final int COLOUR_UNREAD = Color.parseColor("#268bd2"); // Solarized Blue.

	private final BarMover barMover;

	private ScrollIndicator (final BarMover barMover) {
		this.barMover = barMover;
	}

	public void setUnreadTime (final long unreadTime) {
		this.barMover.setUnreadTime(unreadTime);
	}

	public void setUnreadTimeIfNewer (final long unreadTime) {
		if (unreadTime > this.barMover.getUnreadTime()) this.barMover.setUnreadTime(unreadTime);
	}

	public long getUnreadTime () {
		return this.barMover.getUnreadTime();
	}

	public int getUnreadPosition () {
		return this.barMover.getUnreadPosition();
	}

	public static ScrollIndicator attach (final Context context, final ViewGroup rootView, final ListView list, final OnScrollListener onScrollListener) {
		final AbsoluteShape bar = new AbsoluteShape(context, list);
		bar.layoutForce(0, 0, 0, 0);
		bar.setBackgroundColor(COLOUR_BAR);
		bar.bringToFront();
		rootView.addView(bar);

		final BarMover barMovingScrollListener = new BarMover(bar, list, (int) dipToPixels(context, BAR_WIDTH_DIP), onScrollListener);
		list.setOnScrollListener(barMovingScrollListener);

		return new ScrollIndicator(barMovingScrollListener);
	}

	private static float dipToPixels (final Context context, final int a) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, a, context.getResources().getDisplayMetrics());
	}

	/**
	 * @deprecated Replaced by barHeightRelative() which is relative to screen size.
	 */
	@Deprecated
	protected static int barHeightPx (final int position, final float pxPerUnit) {
		return (int) ((position
				+ Math.min(position, ACCEL_STOP_1)
				+ Math.min(position, ACCEL_STOP_2)
				+ Math.min(position, ACCEL_STOP_3)) * pxPerUnit);
	}

	protected static double barHeightRelative (final int position) {
		if (position == 0) return 0d;
		return Math.log(((position / POSITION_MAX_HEIGHT) / SCALE_CONSTANT_THING) + 1d) / LOG_ACCEL_BASE;
	}

	protected static int barHeightPx (final int position, final View view) {
		return (int) (barHeightRelative(position) * view.getHeight());
	}

	private static class AbsoluteShape extends View {

		private final ListView list;
		private final Rect unreadRect;
		private final Paint unreadPaint;
		private int unreadPosition;

		public AbsoluteShape (final Context context, final ListView list) {
			super(context);
			this.list = list;
			this.unreadRect = new Rect(0, 0, 1, 1);
			this.unreadPaint = new Paint(0);
			this.unreadPaint.setColor(COLOUR_UNREAD);
		}

		public void setUnreadPosition (final int unreadPosition) {
			this.unreadPosition = unreadPosition;
			invalidate();
		}

		public int getUnreadPosition () {
			return this.unreadPosition;
		}

		@Override
		public void layout (final int l, final int t, final int r, final int b) {
			/* block calls so no layout manager can not mess with us. */
		}

		public void layoutForce (final int l, final int t, final int r, final int b) {
			super.layout(l, t, r, b);
		}

		@Override
		protected void onSizeChanged (final int w, final int h, final int oldw, final int oldh) {
			this.unreadRect.right = w;
		}

		@Override
		protected void onDraw (final Canvas canvas) {
			super.onDraw(canvas);
			this.unreadRect.bottom = barHeightPx(this.unreadPosition, this.list);
			canvas.drawRect(this.unreadRect, this.unreadPaint);
		}

	}

	private static class BarMover implements OnScrollListener {

		private final AbsoluteShape bar;
		private final ListView list;
		private final TweetListCursorAdapter listAdaptor;
		private final int barWidth;
		private final OnScrollListener delagate;

		private int lastPosition = -1;
		private long unreadTime = -1;

		public BarMover (final AbsoluteShape bar, final ListView list, final int barWidth, final OnScrollListener delagate) {
			this.bar = bar;
			this.list = list;
			this.listAdaptor = (TweetListCursorAdapter) list.getAdapter();
			this.barWidth = barWidth;
			this.delagate = delagate;
		}

		public void setUnreadTime (final long unreadTime) {
			this.unreadTime = unreadTime;
			for (int i = 0; i < this.listAdaptor.getCount(); i++) {
				if (this.listAdaptor.getItemTime(i) <= this.unreadTime) {
					this.bar.setUnreadPosition(i);
					return;
				}
			}
		}

		public long getUnreadTime () {
			return this.unreadTime;
		}

		public int getUnreadPosition () {
			return this.bar.getUnreadPosition();
		}

		private void updateBar () {
			final int position = this.list.getFirstVisiblePosition();
			if (position != this.lastPosition) {
				this.bar.layoutForce(this.list.getRight() - this.barWidth,
						this.list.getTop(),
						this.list.getRight(),
						this.list.getTop() + barHeightPx(position, this.list));
				this.lastPosition = position;

				final long time = this.listAdaptor.getItemTime(position);
				if (time > this.unreadTime) {
					this.unreadTime = time;
					this.bar.setUnreadPosition(position);
				}
			}
		}

		@Override
		public void onScrollStateChanged (final AbsListView view, final int scrollStateFlag) {
			updateBar();
			this.delagate.onScrollStateChanged(view, scrollStateFlag);
		}

		@Override
		public void onScroll (final AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
			updateBar();
			this.delagate.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
		}

	}

}
