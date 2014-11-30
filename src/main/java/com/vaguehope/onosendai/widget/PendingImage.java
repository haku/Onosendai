package com.vaguehope.onosendai.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoadRequest.ImageLoadListener;

public class PendingImage extends FrameLayout {

	private final ExpandingFixedWidthImageView image;
	private final ProgressBar prg;
	private final TextView status;
	private final ExpandingImageLoadListener imageLoadListener;

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

		this.image = new ExpandingFixedWidthImageView(context, this.prg, this.status, maxHeightPixels);
		this.image.setupImageView();
		this.image.setVisibility(View.GONE);
		addView(this.image);

		this.imageLoadListener = new ExpandingImageLoadListener(this.image, this.status, this.prg);
		this.imageLoadListener.imageFetchProgress(0, 0);
	}

	public ImageView getImage () {
		return this.image;
	}

	public ImageLoadListener getImageLoadListener () {
		return this.imageLoadListener;
	}

	private static class ExpandingImageLoadListener implements ImageLoadListener {

		private final ExpandingFixedWidthImageView image;
		private final TextView status;
		private final ProgressBar prg;

		public ExpandingImageLoadListener (final ExpandingFixedWidthImageView image, final TextView status, final ProgressBar prg) {
			this.image = image;
			this.status = status;
			this.prg = prg;
		}

		@Override
		public void imageLoadProgress (final String msg) {
			this.status.setText(msg);
		}

		@Override
		public void imageFetchProgress (final int progress, final int total) {
			if (total > 0) {
				if (this.prg.isIndeterminate()) {
					this.prg.setIndeterminate(false);
					this.prg.setLayoutParams(new FrameLayout.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT,
							Gravity.CENTER));
				}
				this.prg.setMax(total);
				this.prg.setProgress(progress);
			}
			else {
				if (!this.prg.isIndeterminate()) {
					this.prg.setIndeterminate(true);
					this.prg.setLayoutParams(new FrameLayout.LayoutParams(
							ViewGroup.LayoutParams.WRAP_CONTENT,
							ViewGroup.LayoutParams.WRAP_CONTENT,
							Gravity.CENTER));
				}
			}
		}

		@Override
		public void imagePreShow (final ImageLoadRequest req) {
			this.image.resetImageView();
		}

		@Override
		public void imageLoaded (final ImageLoadRequest req) {
			this.image.setVisibility(View.VISIBLE);
			this.prg.setVisibility(View.GONE);
			this.status.setVisibility(View.GONE);
		}

		@Override
		public void imageLoadFailed (final ImageLoadRequest req, final String errMsg) {
			imageLoadProgress(errMsg);
		}

	}

	private static class ExpandingFixedWidthImageView extends FixedWidthImageView {

		private final ProgressBar prg;
		private final TextView status;
		private final int maxHeightPixels;

		public ExpandingFixedWidthImageView (final Context context, final ProgressBar prg, final TextView status, final int maxHeightPixels) {
			super(context);
			this.status = status;
			this.maxHeightPixels = maxHeightPixels;
			this.prg = prg;
		}

		public void setupImageView () {
			if (this.maxHeightPixels > 0) {
				setImageLimitedHeight();
			}
			else {
				setImageFullHeight();
			}
		}

		public void resetImageView () {
			if (this.maxHeightPixels > 0) setImageLimitedHeight();
		}

		public void setImageLimitedHeight () {
			setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, this.maxHeightPixels));
			setScaleType(ScaleType.CENTER_CROP);
			setMaxHeight(this.maxHeightPixels);
			setClickable(true);
			setOnClickListener(new GoFullHeightListener(this));
		}

		public void setImageFullHeight () {
			setMaxHeight(-1);
			setOnClickListener(null);
			setClickable(false);
			setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			setScaleType(ScaleType.FIT_CENTER);
			setAdjustViewBounds(true);
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
				resetImageView();
				super.setImageResource(resId);
				setVisibility(View.VISIBLE);
				this.prg.setVisibility(View.GONE);
				this.status.setVisibility(View.GONE);
			}
		}

	}

	private static class GoFullHeightListener implements OnClickListener {

		private final ExpandingFixedWidthImageView image;

		public GoFullHeightListener (final ExpandingFixedWidthImageView image) {
			this.image = image;
		}

		@Override
		public void onClick (final View v) {
			this.image.setImageFullHeight();
		}

	}

	private static int dipToPixels (final Context context, final int a) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, a, context.getResources().getDisplayMetrics());
	}

}
