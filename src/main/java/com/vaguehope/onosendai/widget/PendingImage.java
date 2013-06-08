package com.vaguehope.onosendai.widget;

import android.content.Context;
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

		final ProgressBar prg = new ProgressBar(context);
		prg.setIndeterminate(true);
		prg.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
		addView(prg);

		this.image = new FixedWidthImageView(context) {

			// FIXME these are icky hacks to get 'show pending' and 'show image' events.

			@Override
			public void setImageResource (final int resId) {
				if (resId == R.drawable.question_blue) {
					setVisibility(View.GONE);
					prg.setVisibility(View.VISIBLE);
				}
				else {
					super.setImageResource(resId);
					setVisibility(View.VISIBLE);
					prg.setVisibility(View.GONE);
				}
			}

			@Override
			public void setImageBitmap (final Bitmap bm) {
				super.setImageBitmap(bm);
				setVisibility(View.VISIBLE);
				prg.setVisibility(View.GONE);
			}

		};
		this.image.setScaleType(ScaleType.FIT_CENTER);
		this.image.setAdjustViewBounds(true);
		this.image.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		this.image.setVisibility(View.GONE);
		addView(this.image);
	}

	public ImageView getImage () {
		return this.image;
	}

}
