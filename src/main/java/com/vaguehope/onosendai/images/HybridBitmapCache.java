package com.vaguehope.onosendai.images;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.vaguehope.onosendai.util.HashHelper;
import com.vaguehope.onosendai.util.HttpHelper.HttpStreamHandler;
import com.vaguehope.onosendai.util.IoHelper;

public class HybridBitmapCache {

	private static final int BASE_HEX = 16;

	private final MemoryBitmapCache<String> memCache;
	private final File baseDir;

	public HybridBitmapCache (final Context context, final int maxMemorySizeBytes) {
		this.memCache = new MemoryBitmapCache<String>(maxMemorySizeBytes);
		this.baseDir = new File(context.getCacheDir(), "images");
		if (!this.baseDir.exists() && !this.baseDir.mkdirs()) throw new IllegalStateException("Failed to create cache directory: " + this.baseDir.getAbsolutePath());
	}

	public Bitmap quickGet (final String key) {
		return this.memCache.get(key);
	}

	public Bitmap get (final String key) {
		Bitmap bmp = this.memCache.get(key);
		if (bmp == null) bmp = getFromDisc(key);
		return bmp;
	}

	public HttpStreamHandler<Bitmap, RuntimeException> fromHttp (final String key) {
		return new DiscCacheHandler(this, key);
	}

	public void clean () {
		this.memCache.evictAll();
	}

	protected Bitmap getFromDisc (final String key) {
		final File file = keyToFile(key);
		if (!file.exists()) return null;
		final Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
		if (bmp != null) this.memCache.put(key, bmp);
		return bmp;
	}

	protected File keyToFile (final String key) {
		return new File(this.baseDir, HashHelper.md5String(key).toString(BASE_HEX));
	}

	private static class DiscCacheHandler implements HttpStreamHandler<Bitmap, RuntimeException> {

		private final HybridBitmapCache cache;
		private final String key;

		public DiscCacheHandler (final HybridBitmapCache cache, final String key) {
			this.cache = cache;
			this.key = key;
		}

		@Override
		public Bitmap handleStream (final InputStream is, final int contentLength) throws IOException {
			final OutputStream os = new FileOutputStream(this.cache.keyToFile(this.key));
			try {
				IoHelper.copy(is, os);
			}
			finally {
				os.close();
			}
			// TODO replace write+read disc with keep bytes in memory?
			return this.cache.getFromDisc(this.key);
		}

	}

}
