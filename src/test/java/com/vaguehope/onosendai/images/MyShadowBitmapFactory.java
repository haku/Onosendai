package com.vaguehope.onosendai.images;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

@Implements(BitmapFactory.class)
public class MyShadowBitmapFactory {

	/**
	 * @param pathName
	 */
	@Implementation
	public static Bitmap decodeFile(final String pathName, final BitmapFactory.Options options) {
		if (!options.inJustDecodeBounds) throw new IllegalArgumentException("only inJustDecodeBounds is supported.");
		options.outMimeType = "image/png";
		return null;
	}

}
