package com.vaguehope.onosendai.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;

import com.vaguehope.onosendai.R;

public class PendingImage extends FrameLayout {

	private final FixedWidthImageView image;

	public PendingImage (final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PendingImage (final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PendingImage);
		final int maxHeightPixels = a.getDimensionPixelSize(R.styleable.PendingImage_maxHeight, -1);
		a.recycle();

		final ProgressBar prg = new ProgressBar(context);
		prg.setIndeterminate(true);
		prg.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
		addView(prg);

		this.image = new ProgressAwareFixedWidthImageView(context, prg, maxHeightPixels);
		setupImageView(this.image, maxHeightPixels);
		this.image.setVisibility(View.GONE);
		addView(this.image);
	}

	public ImageView getImage () {
		return this.image;
	}

	private static void setupImageView (final FixedWidthImageView image, final int maxHeightPixels) {
		if (maxHeightPixels > 0) {
			setImageLimitedHeight(image, maxHeightPixels);
		}
		else {
			setImageFullHeight(image);
		}
	}

	protected static void resetImageView (final FixedWidthImageView image, final int maxHeightPixels) {
		if (maxHeightPixels > 0) setImageLimitedHeight(image, maxHeightPixels);
	}

	private static void setImageLimitedHeight (final FixedWidthImageView image, final int maxHeightPixels) {
		image.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxHeightPixels));
		image.setScaleType(ScaleType.CENTER_CROP);
		image.setMaxHeight(maxHeightPixels);
		image.setClickable(true);
		image.setOnClickListener(new GoFullHeightListener(image));
	}

	protected static void setImageFullHeight (final FixedWidthImageView image) {
		image.setMaxHeight(-1);
		image.setOnClickListener(null);
		image.setClickable(false);
		image.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		image.setScaleType(ScaleType.FIT_CENTER);
		image.setAdjustViewBounds(true);
	}

	private static class ProgressAwareFixedWidthImageView extends FixedWidthImageView {

		private final ProgressBar prg;
		private final int maxHeightPixels;

		public ProgressAwareFixedWidthImageView (final Context context, final ProgressBar prg, final int maxHeightPixels) {
			super(context);
			this.maxHeightPixels = maxHeightPixels;
			this.prg = prg;
		}

		// XXX these are icky hacks to get 'show pending' and 'show image' events.

		@Override
		public void setImageResource (final int resId) {
			if (resId == R.drawable.question_blue) {
				setVisibility(View.GONE);
				this.prg.setVisibility(View.VISIBLE);
			}
			else {
				resetImageView(this, this.maxHeightPixels);
				super.setImageResource(resId);
				setVisibility(View.VISIBLE);
				this.prg.setVisibility(View.GONE);
			}
		}

		@Override
		public void setImageBitmap (final Bitmap bm) {
			resetImageView(this, this.maxHeightPixels);
			super.setImageBitmap(bm);
			setVisibility(View.VISIBLE);
			this.prg.setVisibility(View.GONE);
		}

	}

	private static class GoFullHeightListener implements OnClickListener {

		private final FixedWidthImageView image;

		public GoFullHeightListener (final FixedWidthImageView image) {
			this.image = image;
		}

		@Override
		public void onClick (final View v) {
			setImageFullHeight(this.image);
		}

	}

}
