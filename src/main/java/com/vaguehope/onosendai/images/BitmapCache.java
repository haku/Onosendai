package com.vaguehope.onosendai.images;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

public class BitmapCache<K> extends LruCache<K, Bitmap> {

	public BitmapCache (final int maxSizeBytes) {
		super(maxSizeBytes);
	}

	@Override
	protected int sizeOf (final K key, final Bitmap value) {
		return value.getByteCount();
	}

	@Override
	protected void entryRemoved (final boolean evicted, final K key, final Bitmap oldValue, final Bitmap newValue) {
		oldValue.recycle();
	}

}