package com.vaguehope.onosendai.images;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.util.HashHelper;
import com.vaguehope.onosendai.util.HttpHelper.HttpStreamHandler;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.LogWrapper;

public class HybridBitmapCache {

	private static final int BASE_HEX = 16;
	static LogWrapper LOG = new LogWrapper("HC");

	private final Context context;
	private final MemoryBitmapCache<String> memCache;
	private final File baseDir;

	public HybridBitmapCache (final Context context, final int maxMemorySizeBytes) {
		this.context = context;
		this.memCache = new MemoryBitmapCache<String>(maxMemorySizeBytes);
		this.baseDir = new File(context.getCacheDir(), "images");
		if (!this.baseDir.exists() && !this.baseDir.mkdirs()) throw new IllegalStateException("Failed to create cache directory: " + this.baseDir.getAbsolutePath());
	}

	public Bitmap quickGet (final String key) {
		return this.memCache.get(key);
	}

	/**
	 * @return null if image is not in cache.
	 * @throws UnrederableException if image is in cache but can not be rendered.
	 */
	public Bitmap get (final String key) throws UnrederableException {
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

	protected Bitmap getFromDisc (final String key) throws UnrederableException {
		if (key == null) return null;
		final File file = keyToFile(key);
		if (!file.exists()) return null;
		final Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
		if (bmp == null) {
			this.memCache.put(key, BitmapFactory.decodeResource(this.context.getResources(), R.drawable.exclamation_red));
			throw new UnrederableException(file);
		}
		this.memCache.put(key, bmp);
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
			final File f = this.cache.keyToFile(this.key);
			final OutputStream os = new FileOutputStream(f);
			try {
				IoHelper.copy(is, os);
			}
			catch (IOException e) {
				if (!f.delete()) LOG.e("Failed to delete incomplete download '" + f.getAbsolutePath() + "': " + e.toString());
				throw e;
			}
			finally {
				os.close();
			}
			return this.cache.getFromDisc(this.key);
		}

	}

}
