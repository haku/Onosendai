package com.vaguehope.onosendai.images;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

public class MemoryBitmapCache<K> extends LruCache<K, Bitmap> {

	public MemoryBitmapCache (final int maxSizeBytes) {
		super(maxSizeBytes);
	}

	@Override
	protected int sizeOf (final K key, final Bitmap value) {
		return bmpByteCount(value);
	}

	public static int bmpByteCount (final Bitmap value) {
		return value.getRowBytes() * value.getHeight(); // Backwards compatible equivalent of getByteCount().
	}

}