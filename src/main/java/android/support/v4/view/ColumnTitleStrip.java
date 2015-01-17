/* Hacked based on android.support.v4.view.PagerTitleStrip.
 * Which is Apache 2 license.
 */

package android.support.v4.view;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import com.vaguehope.onosendai.util.LogWrapper;

public class ColumnTitleStrip extends ViewGroup implements ViewPager.Decor /* Decor is package-private :( */{

	private static final LogWrapper LOG = new LogWrapper("CTS");

	private final PageListener mPageListener = new PageListener();
	private ViewPager mPager;
	private WeakReference<PagerAdapter> mWatchingAdapter;

	private int mLastKnownCurrentPage = -1;
	private float mLastKnownPositionOffset = -1;

	private boolean mUpdatingText;
	private boolean mUpdatingPositions;

	private final List<TextView> txtLabels = new ArrayList<TextView>();
	private float relativePageWidth = 1.f;

	public ColumnTitleStrip (final Context context) {
		this(context, null);
	}

	public ColumnTitleStrip (final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onAttachedToWindow () {
		super.onAttachedToWindow();

		final ViewParent parent = getParent();
		if (!(parent instanceof ViewPager)) throw new IllegalStateException("PagerTitleStrip must be a direct child of a ViewPager.");

		final ViewPager pager = (ViewPager) parent;
		pager.setInternalPageChangeListener(this.mPageListener);
		pager.setOnAdapterChangeListener(this.mPageListener);
		this.mPager = pager;
		updateAdapter(this.mWatchingAdapter != null ? this.mWatchingAdapter.get() : null, pager.getAdapter());
	}

	@Override
	protected void onDetachedFromWindow () {
		super.onDetachedFromWindow();
		if (this.mPager != null) {
			updateAdapter(this.mPager.getAdapter(), null);
			this.mPager.setInternalPageChangeListener(null);
			this.mPager.setOnAdapterChangeListener(null);
			this.mPager = null;
		}
	}

	/**
	 * TODO may be able to factor out this not poorly-named method.
	 */
	void updateText (final int currentItem) {
		this.mUpdatingText = true;
		this.mLastKnownCurrentPage = currentItem;
		if (!this.mUpdatingPositions) updateTextPositions(currentItem, this.mLastKnownPositionOffset, false);
		this.mUpdatingText = false;
	}

	@Override
	public void requestLayout () {
		if (!this.mUpdatingText) super.requestLayout();
	}

	void updateAdapter (final PagerAdapter oldAdapter, final PagerAdapter newAdapter) {
		if (oldAdapter != null) {
			oldAdapter.unregisterDataSetObserver(this.mPageListener);
			this.mWatchingAdapter = null;
		}
		if (newAdapter != null) {
			newAdapter.registerDataSetObserver(this.mPageListener);
			this.mWatchingAdapter = new WeakReference<PagerAdapter>(newAdapter);
			createWidgets(newAdapter);
		}
		if (this.mPager != null) {
			this.mLastKnownCurrentPage = -1;
			this.mLastKnownPositionOffset = -1;
			updateText(this.mPager.getCurrentItem());
			requestLayout();
		}
	}

	private void createWidgets (final PagerAdapter adapter) {
		this.relativePageWidth = adapter.getPageWidth(Integer.MIN_VALUE); // TODO for now assume all columns same width.

		if (this.txtLabels.size() < adapter.getCount()) {
			while (this.txtLabels.size() < adapter.getCount()) {
				final TextView tv = new TextView(getContext());
				this.txtLabels.add(tv);
				addView(tv);
			}
		}
		else if (this.txtLabels.size() > adapter.getCount()) {
			while (this.txtLabels.size() > adapter.getCount()) {
				removeView(this.txtLabels.remove(this.txtLabels.size() - 1));
			}
		}

		final int width = getWidth() - getPaddingLeft() - getPaddingRight(); // TODO FIXME take into account getPageWidth().
		final int childHeight = getHeight() - getPaddingTop() - getPaddingBottom();
		final int childWidthSpec = MeasureSpec.makeMeasureSpec((int) (width * 0.8f), MeasureSpec.AT_MOST);
		final int childHeightSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.AT_MOST);

		for (int i = 0; i < this.txtLabels.size(); i++) {
			final TextView tv = this.txtLabels.get(i);
			tv.setText(adapter.getPageTitle(i));
			tv.measure(childWidthSpec, childHeightSpec);
		}
	}

	void updateTextPositions (final int position, final float positionOffset, final boolean force) {
		if (position != this.mLastKnownCurrentPage) {
			updateText(position);
		}
		else if (!force && positionOffset == this.mLastKnownPositionOffset) {
			return;
		}
		this.mUpdatingPositions = true;

		final int stripWidth = getWidth();
		final int stripHeight = getHeight();
		final int paddingLeft = getPaddingLeft();
		final int paddingRight = getPaddingRight();
		final int paddingBottom = getPaddingBottom();
		final int stripInnerWidth = stripWidth - paddingLeft - paddingRight;

		final int columnWidth = (int) (stripInnerWidth * this.relativePageWidth); // FIXME rounding error may be introduced.
		final int halfColumnWidth = columnWidth / 2;
		final int firstColumnLeft = 0 - (columnWidth * position);

		for (int i = 0; i < this.txtLabels.size(); i++) {
			final TextView tv = this.txtLabels.get(i);

			final int tWidth = tv.getMeasuredWidth();
			final int tLeft = firstColumnLeft + (columnWidth * i) + halfColumnWidth - (tWidth / 2) - (int) (positionOffset * columnWidth);
			final int bottomGravTop = stripHeight - paddingBottom - tv.getMeasuredHeight();

			tv.layout(tLeft, bottomGravTop, tLeft + tWidth, bottomGravTop + tv.getMeasuredHeight());
		}

		this.mLastKnownPositionOffset = positionOffset;
		this.mUpdatingPositions = false;
	}

	@Override
	protected void onMeasure (final int widthMeasureSpec, final int heightMeasureSpec) {
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		if (widthMode != MeasureSpec.EXACTLY) throw new IllegalStateException("Must measure with an exact width.");

		final int padding = getPaddingTop() + getPaddingBottom();
		final int childHeight = heightSize - padding;

		final int childWidthSpec = MeasureSpec.makeMeasureSpec((int) (widthSize * 0.8f), MeasureSpec.AT_MOST);
		final int childHeightSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.AT_MOST);

		// Cascade measuring.
		int maxTextHeight = 1;
		for (final TextView tv : this.txtLabels) {
			tv.measure(childWidthSpec, childHeightSpec);
			maxTextHeight = Math.max(maxTextHeight, tv.getMeasuredHeight());
		}

		if (heightMode == MeasureSpec.EXACTLY) {
			setMeasuredDimension(widthSize, heightSize);
		}
		else {
			setMeasuredDimension(widthSize, Math.max(getMinHeight(), maxTextHeight + padding));
		}
	}

	@Override
	protected void onLayout (final boolean changed, final int l, final int t, final int r, final int b) {
		if (this.mPager != null) {
			final float offset = this.mLastKnownPositionOffset >= 0 ? this.mLastKnownPositionOffset : 0;
			updateTextPositions(this.mLastKnownCurrentPage, offset, true);
		}
	}

	int getMinHeight () {
		int minHeight = 0;
		final Drawable bg = getBackground();
		if (bg != null) {
			minHeight = bg.getIntrinsicHeight();
		}
		return minHeight;
	}

	private class PageListener extends DataSetObserver implements ViewPager.OnPageChangeListener, ViewPager.OnAdapterChangeListener {
		private int mScrollState;

		@Override
		public void onPageScrolled (final int position, final float positionOffset, final int positionOffsetPixels) {
			updateTextPositions(position, positionOffset, false);
		}

		@Override
		public void onPageSelected (final int position) {
			if (this.mScrollState == ViewPager.SCROLL_STATE_IDLE) {
				// Only update the text here if we're not dragging or settling.
				updateText(ColumnTitleStrip.this.mPager.getCurrentItem());

				final float offset = ColumnTitleStrip.this.mLastKnownPositionOffset >= 0 ? ColumnTitleStrip.this.mLastKnownPositionOffset : 0;
				updateTextPositions(ColumnTitleStrip.this.mPager.getCurrentItem(), offset, true);
			}
		}

		@Override
		public void onPageScrollStateChanged (final int state) {
			this.mScrollState = state;
		}

		@Override
		public void onAdapterChanged (final PagerAdapter oldAdapter, final PagerAdapter newAdapter) {
			updateAdapter(oldAdapter, newAdapter);
		}

		@Override
		public void onChanged () {
			updateText(ColumnTitleStrip.this.mPager.getCurrentItem());

			final float offset = ColumnTitleStrip.this.mLastKnownPositionOffset >= 0 ? ColumnTitleStrip.this.mLastKnownPositionOffset : 0;
			updateTextPositions(ColumnTitleStrip.this.mPager.getCurrentItem(), offset, true);
		}
	}

}
