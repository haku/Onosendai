/* Hacked based on android.support.v4.view.PagerTitleStrip.
 * Which is Apache 2 license.
 */

package android.support.v4.view;

import java.lang.ref.WeakReference;

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

	TextView mCurrText;

	public ColumnTitleStrip (final Context context) {
		this(context, null);
	}

	public ColumnTitleStrip (final Context context, final AttributeSet attrs) {
		super(context, attrs);

		addView(this.mCurrText = new TextView(context));
	}

	@Override
	protected void onAttachedToWindow () {
		super.onAttachedToWindow();

		final ViewParent parent = getParent();
		if (!(parent instanceof ViewPager)) throw new IllegalStateException("PagerTitleStrip must be a direct child of a ViewPager.");

		final ViewPager pager = (ViewPager) parent;
		final PagerAdapter adapter = pager.getAdapter();

		pager.setInternalPageChangeListener(this.mPageListener);
		pager.setOnAdapterChangeListener(this.mPageListener);
		this.mPager = pager;
		updateAdapter(this.mWatchingAdapter != null ? this.mWatchingAdapter.get() : null, adapter);
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

	void updateText (final int currentItem, final PagerAdapter adapter) {
		final int itemCount = adapter != null ? adapter.getCount() : 0;
		this.mUpdatingText = true;

		this.mCurrText.setText(adapter != null && currentItem < itemCount ? adapter.getPageTitle(currentItem) : null);

		// Measure everything
		final int width = getWidth() - getPaddingLeft() - getPaddingRight();
		final int childHeight = getHeight() - getPaddingTop() - getPaddingBottom();
		final int childWidthSpec = MeasureSpec.makeMeasureSpec((int) (width * 0.8f), MeasureSpec.AT_MOST);
		final int childHeightSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.AT_MOST);
		this.mCurrText.measure(childWidthSpec, childHeightSpec);

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
		}
		if (this.mPager != null) {
			this.mLastKnownCurrentPage = -1;
			this.mLastKnownPositionOffset = -1;
			updateText(this.mPager.getCurrentItem(), newAdapter);
			requestLayout();
		}
	}

	void updateTextPositions (final int position, final float positionOffset, final boolean force) {
		if (position != this.mLastKnownCurrentPage) {
			updateText(position, this.mPager.getAdapter());
		}
		else if (!force && positionOffset == this.mLastKnownPositionOffset) {
			return;
		}

		this.mUpdatingPositions = true;

		final int currWidth = this.mCurrText.getMeasuredWidth();
		final int halfCurrWidth = currWidth / 2;

		final int stripWidth = getWidth();
		final int stripHeight = getHeight();
		final int paddingLeft = getPaddingLeft();
		final int paddingRight = getPaddingRight();
		final int paddingBottom = getPaddingBottom();
		final int textPaddedLeft = paddingLeft + halfCurrWidth;
		final int textPaddedRight = paddingRight + halfCurrWidth;
		final int contentWidth = stripWidth - textPaddedLeft - textPaddedRight;

		float currOffset = positionOffset + 0.5f;
		if (currOffset > 1.f) currOffset -= 1.f;

		final int currCenter = stripWidth - textPaddedRight - (int) (contentWidth * currOffset);
		final int currLeft = currCenter - currWidth / 2;
		final int currRight = currLeft + currWidth;

		final int bottomGravTop = stripHeight - paddingBottom - this.mCurrText.getMeasuredHeight();
		this.mCurrText.layout(currLeft, bottomGravTop, currRight, bottomGravTop + this.mCurrText.getMeasuredHeight());

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

		this.mCurrText.measure(childWidthSpec, childHeightSpec);

		if (heightMode == MeasureSpec.EXACTLY) {
			setMeasuredDimension(widthSize, heightSize);
		}
		else {
			final int textHeight = this.mCurrText.getMeasuredHeight();
			setMeasuredDimension(widthSize, Math.max(getMinHeight(), textHeight + padding));
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
		public void onPageScrolled (int position, final float positionOffset, final int positionOffsetPixels) {
			if (positionOffset > 0.5f) position++; // Consider ourselves to be on the next page when we're 50% of the way there.
			updateTextPositions(position, positionOffset, false);
		}

		@Override
		public void onPageSelected (final int position) {
			if (this.mScrollState == ViewPager.SCROLL_STATE_IDLE) {
				// Only update the text here if we're not dragging or settling.
				updateText(ColumnTitleStrip.this.mPager.getCurrentItem(), ColumnTitleStrip.this.mPager.getAdapter());

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
			updateText(ColumnTitleStrip.this.mPager.getCurrentItem(), ColumnTitleStrip.this.mPager.getAdapter());

			final float offset = ColumnTitleStrip.this.mLastKnownPositionOffset >= 0 ? ColumnTitleStrip.this.mLastKnownPositionOffset : 0;
			updateTextPositions(ColumnTitleStrip.this.mPager.getCurrentItem(), offset, true);
		}
	}

}
