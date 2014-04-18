package com.vaguehope.onosendai.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoadRequest.ImageLoadListener;

public class PendingImage extends FrameLayout {

	private final FixedWidthImageView image;
	private final ProgressBar prg;
	private final TextView status;

	public PendingImage (final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PendingImage (final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);

		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PendingImage);
		final int maxHeightPixels = a.getDimensionPixelSize(R.styleable.PendingImage_maxHeight, -1)
				- getPaddingTop() - getPaddingBottom();
		a.recycle();

		this.prg = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
		this.imageLoadListener.imageFetchProgress(0, 0);
		addView(this.prg);

		this.status = new TextView(context);
		this.status.setLayoutParams(new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT,
				Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM));
		this.status.setPadding(
				this.status.getPaddingLeft() + dipToPixels(context, 10),
				this.status.getPaddingTop(),
				this.status.getPaddingRight() + dipToPixels(context, 10),
				this.status.getPaddingBottom() + dipToPixels(context, 20));
		addView(this.status);

		this.image = new ProgressAwareFixedWidthImageView(context, this.prg, this.status, maxHeightPixels);
		setupImageView(this.image, maxHeightPixels);
		this.image.setVisibility(View.GONE);
		addView(this.image);
	}

	public ImageView getImage () {
		return this.image;
	}

	public ImageLoadListener getImageLoadListener () {
		return this.imageLoadListener;
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

	private final ImageLoadListener imageLoadListener = new ImageLoadListener() {

		@Override
		public void imageLoadProgress (final String msg) {
			PendingImage.this.status.setText(msg);
		}

		@Override
		public void imageFetchProgress (final int progress, final int total) {
			if (total > 0) {
				if (PendingImage.this.prg.isIndeterminate()) {
					PendingImage.this.prg.setIndeterminate(false);
					PendingImage.this.prg.setLayoutParams(new FrameLayout.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT,
							Gravity.CENTER));
				}
				PendingImage.this.prg.setMax(total);
				PendingImage.this.prg.setProgress(progress);
			}
			else {
				if (!PendingImage.this.prg.isIndeterminate()) {
					PendingImage.this.prg.setIndeterminate(true);
					PendingImage.this.prg.setLayoutParams(new FrameLayout.LayoutParams(
							ViewGroup.LayoutParams.WRAP_CONTENT,
							ViewGroup.LayoutParams.WRAP_CONTENT,
							Gravity.CENTER));
				}
			}
		}

		@Override
		public void imageLoaded (final ImageLoadRequest req) {}

		@Override
		public void imageLoadFailed (final ImageLoadRequest req, final String errMsg) {
			imageLoadProgress(errMsg);
		}

	};

	private static class ProgressAwareFixedWidthImageView extends FixedWidthImageView {

		private final ProgressBar prg;
		private final TextView status;
		private final int maxHeightPixels;

		public ProgressAwareFixedWidthImageView (final Context context, final ProgressBar prg, final TextView status, final int maxHeightPixels) {
			super(context);
			this.status = status;
			this.maxHeightPixels = maxHeightPixels;
			this.prg = prg;
		}

		// XXX these are icky hacks to get 'show pending' and 'show image' events.

		@Override
		public void setImageResource (final int resId) {
			if (resId == R.drawable.question_blue) {
				super.setImageDrawable(null);
				setVisibility(View.GONE);
				this.prg.setVisibility(View.VISIBLE);
				this.status.setVisibility(View.VISIBLE);
			}
			else if (resId == R.drawable.exclamation_red) {
				super.setImageDrawable(null);
				setVisibility(View.GONE);
				this.prg.setVisibility(View.GONE);
				this.status.setVisibility(View.VISIBLE);
			}
			else {
				resetImageView(this, this.maxHeightPixels);
				super.setImageResource(resId);
				setVisibility(View.VISIBLE);
				this.prg.setVisibility(View.GONE);
				this.status.setVisibility(View.GONE);
			}
		}

		@Override
		public void setImageBitmap (final Bitmap bm) {
			resetImageView(this, this.maxHeightPixels);
			super.setImageBitmap(bm);
			setVisibility(View.VISIBLE);
			this.prg.setVisibility(View.GONE);
			this.status.setVisibility(View.GONE);
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

	private static int dipToPixels (final Context context, final int a) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, a, context.getResources().getDisplayMetrics());
	}

}
