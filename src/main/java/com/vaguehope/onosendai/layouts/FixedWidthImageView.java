package com.vaguehope.onosendai.layouts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class FixedWidthImageView extends ImageView {

	public FixedWidthImageView (final Context context) {
		super(context);
	}

	public FixedWidthImageView (final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	public FixedWidthImageView (final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure (final int widthMeasureSpec, final int heightMeasureSpec) {
		final int width = MeasureSpec.getSize(widthMeasureSpec);
		final Drawable d = getDrawable();
		final int height = width * d.getIntrinsicHeight() / d.getIntrinsicWidth();
		setMeasuredDimension(width, height);
	}

}
