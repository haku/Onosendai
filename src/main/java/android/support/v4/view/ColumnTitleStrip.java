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
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.vaguehope.onosendai.R;

public class ColumnTitleStrip extends ViewGroup implements ViewPager.Decor /* Decor is package-private :( */{

	public interface ColumnClickListener {
		void onColumnTitleClick (int position);
	}

	private static final int[] ATTRS = new int[] {
			android.R.attr.textSize
	};

	private final int textSizePx;

	private final PageListener mPageListener = new PageListener();
	private ViewPager pager;
	private WeakReference<PagerAdapter> mWatchingAdapter;
	private ColumnClickListener columnClickListener;

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

	public void setViewPager (final ViewPager newPager) {
		if (this.pager != null) {
			updateAdapter(this.pager.getAdapter(), null);
			this.pager.setInternalPageChangeListener(null);
			this.pager.setOnAdapterChangeListener(null);
		}
		this.pager = newPager;
		if (this.pager != null) {
			this.pager.setInternalPageChangeListener(this.mPageListener);
			this.pager.setOnAdapterChangeListener(this.mPageListener);
			updateAdapter(this.mWatchingAdapter != null ? this.mWatchingAdapter.get() : null, this.pager.getAdapter());
		}
	}

	public void setColumnClickListener (final ColumnClickListener listener) {
		this.columnClickListener = listener;
	}

	/**
	 * Null title to clear.
	 */
	public void setTempColumnTitle(final int position, final String title) {
		final TextView tv = this.labels.get(position);
		if (tv == null) return;
		tv.setText(title != null ? title : String.valueOf(tv.getTag()));
	}

	@Override
	protected void onDetachedFromWindow () {
		super.onDetachedFromWindow();
		setViewPager(null);
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
		if (this.pager != null) {
			this.mLastKnownCurrentPage = -1;
			this.mLastKnownPositionOffset = -1;
			updateText(this.pager.getCurrentItem());
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
				tv.setOnClickListener(new TitleClickListener(this, this.labels.size()));
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
			final CharSequence title = adapter.getPageTitle(i);
			tv.setText(title);
			tv.setTag(title);
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

		// XXX Use pager's width.
		final int contentWidth = this.pager.getWidth() - getPaddingLeft() - getPaddingRight();
		final int columnWidth = (int) (contentWidth * this.relativePageWidth); // FIXME rounding error may be introduced.
		final int firstColumnLeft = 0 - (columnWidth * position) - getLeft(); // XXX Offset Left.

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

		// XXX Use pager's width.
		final int contentWidth = this.pager.getWidth() - getPaddingLeft() - getPaddingRight();
		final int columnWidth = (int) (contentWidth * this.relativePageWidth); // FIXME rounding error may be introduced.

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
		if (this.pager != null) {
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
			updateText(ColumnTitleStrip.this.pager.getCurrentItem());

			final float offset = ColumnTitleStrip.this.mLastKnownPositionOffset >= 0 ? ColumnTitleStrip.this.mLastKnownPositionOffset : 0;
			updateTextPositions(ColumnTitleStrip.this.pager.getCurrentItem(), offset, true);
		}
	}

	private static class TitleClickListener implements OnClickListener {

		private final ColumnTitleStrip host;
		private final int position;

		public TitleClickListener (final ColumnTitleStrip host, final int position) {
			this.host = host;
			this.position = position;
		}

		@Override
		public void onClick (final View v) {
			this.host.columnClickListener.onColumnTitleClick(this.position);
		}

	}

}
