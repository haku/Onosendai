/* Hacked based on android.support.v4.view.PagerTitleStrip.
 * Which is Apache 2 license.
 */

package android.support.v4.view;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.util.LogWrapper;

public class ColumnTitleStrip extends ViewGroup implements ViewPager.Decor /* Decor is package-private :( */{

	private static final int[] ATTRS = new int[] {
			android.R.attr.textSize
	};

	private static final LogWrapper LOG = new LogWrapper("CTS");

	private final int textSizePx;

	private final PageListener mPageListener = new PageListener();
	private ViewPager mPager;
	private WeakReference<PagerAdapter> mWatchingAdapter;

	private int mLastKnownCurrentPage = -1;
	private float mLastKnownPositionOffset = -1;

	private boolean mUpdatingText;
	private boolean mUpdatingPositions;

	private final List<TextView> labels = new ArrayList<TextView>();
	private float relativePageWidth = 1.f;

	public ColumnTitleStrip (final Context context) {
		this(context, null);
	}

	public ColumnTitleStrip (final Context context, final AttributeSet attrs) {
		super(context, attrs);

		final TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);
		this.textSizePx = a.getDimensionPixelSize(1, 0);
		a.recycle();
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

		if (this.labels.size() < adapter.getCount()) {
			LayoutInflater inflater = null;
			while (this.labels.size() < adapter.getCount()) {
				if (inflater == null) inflater = LayoutInflater.from(getContext());
				final TextView tv = (TextView) inflater.inflate(R.layout.titlestripbutton, this, false);
				if (this.textSizePx != 0) tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.textSizePx);
				this.labels.add(tv);
				addView(tv);
			}
		}
		else if (this.labels.size() > adapter.getCount()) {
			while (this.labels.size() > adapter.getCount()) {
				removeView(this.labels.remove(this.labels.size() - 1));
			}
		}

		final int width = getWidth() - getPaddingLeft() - getPaddingRight(); // TODO FIXME take into account getPageWidth().
		final int childHeight = getHeight() - getPaddingTop() - getPaddingBottom();
		final int childWidthSpec = MeasureSpec.makeMeasureSpec((int) (width * 0.8f), MeasureSpec.AT_MOST);
		final int childHeightSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.AT_MOST);

		for (int i = 0; i < this.labels.size(); i++) {
			final TextView tv = this.labels.get(i);
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

		final int columnWidth = (int) ((getWidth() - getPaddingLeft() - getPaddingRight()) * this.relativePageWidth); // FIXME rounding error may be introduced.
		final int firstColumnLeft = 0 - (columnWidth * position);

		for (int i = 0; i < this.labels.size(); i++) {
			final TextView tv = this.labels.get(i);
			final int tLeft = firstColumnLeft + (columnWidth * i) - (int) (positionOffset * columnWidth);
			final int bottomGravTop = getHeight() - getPaddingBottom() - tv.getMeasuredHeight();
			tv.layout(tLeft, bottomGravTop, tLeft + columnWidth, bottomGravTop + tv.getMeasuredHeight());
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

		final int columnWidth = (int) ((widthSize - getPaddingLeft() - getPaddingRight()) * this.relativePageWidth); // FIXME rounding error may be introduced.

		final int vPadding = getPaddingTop() + getPaddingBottom();
		final int childHeight = heightSize - vPadding;

		final int childWidthSpec = MeasureSpec.makeMeasureSpec(columnWidth, MeasureSpec.EXACTLY);
		final int childHeightSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.AT_MOST);

		// Cascade measuring.
		int maxTextHeight = 1;
		for (final TextView tv : this.labels) {
			tv.measure(childWidthSpec, childHeightSpec);
			maxTextHeight = Math.max(maxTextHeight, tv.getMeasuredHeight());
		}

		if (heightMode == MeasureSpec.EXACTLY) {
			setMeasuredDimension(widthSize, heightSize);
		}
		else {
			setMeasuredDimension(widthSize, Math.max(getMinHeight(), maxTextHeight + vPadding));
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

		@Override
		public void onPageScrolled (final int position, final float positionOffset, final int positionOffsetPixels) {
			updateTextPositions(position, positionOffset, false);
		}

		@Override
		public void onPageSelected (final int position) {/* Unused. */}

		@Override
		public void onPageScrollStateChanged (final int state) {/* Unused */}

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
