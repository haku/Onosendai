package com.vaguehope.onosendai.images;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

public class MemoryBitmapCache<K> extends LruCache<K, Bitmap> {

	public MemoryBitmapCache (final int maxSizeBytes) {
		super(maxSizeBytes);
	}

	@Override
	protected int sizeOf (final K key, final Bitmap value) {
		return value.getRowBytes() * value.getHeight(); // Backwards compatible equivalent of getByteCount().
	}

	@Override
	protected void entryRemoved (final boolean evicted, final K key, final Bitmap oldValue, final Bitmap newValue) {
		// In retrospect, this is probably a bad idea as the image may still be in the UI, etc.  Disabling.
		//oldValue.recycle();
	}

}