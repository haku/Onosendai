package com.vaguehope.onosendai.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.vaguehope.onosendai.R;

public class ClickToExpand extends FrameLayout {

	private final int maxHeightPixels;

	private boolean expanded = false;

	public ClickToExpand (final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ClickToExpand (final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);

		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PendingImage);
		this.maxHeightPixels = a.getDimensionPixelSize(R.styleable.PendingImage_maxHeight, -1) - getPaddingTop() - getPaddingBottom();
		a.recycle();

		setOnClickListener(new GoFullHeightListener(this));
		setExpanded(false);
	}

	@Override
	public void setLayoutParams (final android.view.ViewGroup.LayoutParams params) {
		super.setLayoutParams(params);
		setExpanded(this.expanded);
	}

	@Override
	protected void onMeasure (final int widthMeasureSpec, final int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec,
				this.expanded
						? heightMeasureSpec
						: MeasureSpec.makeMeasureSpec(this.maxHeightPixels, MeasureSpec.EXACTLY));
	}

	public void setExpanded (final boolean expanded) {
		this.expanded = expanded;
		setClickable(!expanded);
		final ViewGroup.LayoutParams lp = getLayoutParams();
		if (lp != null) {
			lp.height = expanded ? ViewGroup.LayoutParams.WRAP_CONTENT : this.maxHeightPixels;
			requestLayout();
		}
	}

	/**
	 * TODO Lazy initial impl assumes only one child and always visible.
	 */
	private boolean expansionRequired () {
		return getChildCount() > 0 && getChildAt(0).getMeasuredHeight() >= this.maxHeightPixels;
	}

	@Override
	public boolean onInterceptTouchEvent (final MotionEvent ev) {
		final boolean eatClick = !this.expanded && expansionRequired();
		setClickable(eatClick);
		return eatClick;
	}

	private static class GoFullHeightListener implements OnClickListener {

		private final ClickToExpand view;

		public GoFullHeightListener (final ClickToExpand view) {
			this.view = view;
		}

		@Override
		public void onClick (final View v) {
			this.view.setExpanded(true);
		}

	}

}
