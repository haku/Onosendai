package com.vaguehope.onosendai.images;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.util.HashHelper;
import com.vaguehope.onosendai.util.HttpHelper.HttpStreamHandler;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.SyncMgr;

public class HybridBitmapCache {

	private static final int BASE_HEX = 16;
	static final LogWrapper LOG = new LogWrapper("HC");

	private final Context context;
	private final MemoryBitmapCache<String> memCache;
	private final File baseDir;
	private final SyncMgr syncMgr;

	public HybridBitmapCache (final Context context, final int maxMemorySizeBytes) {
		this.context = context;
		this.memCache = new MemoryBitmapCache<String>(maxMemorySizeBytes);
		this.baseDir = getBaseDir(context);
		if (!this.baseDir.exists() && !this.baseDir.mkdirs()) throw new IllegalStateException("Failed to create cache directory: " + this.baseDir.getAbsolutePath());
		this.syncMgr = new SyncMgr();
	}

	public Bitmap quickGet (final String key) {
		return this.memCache.get(key);
	}

	/**
	 * @return null if image is not in cache.
	 * @throws UnrederableException
	 *             if image is in cache but can not be rendered.
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
		refreshFileTimestamp(file);
		return bmp;
	}

	protected File keyToFile (final String key) {
		return new File(this.baseDir, HashHelper.md5String(key).toString(BASE_HEX));
	}

	public SyncMgr getSyncMgr () {
		return this.syncMgr;
	}

	private static void refreshFileTimestamp (final File f) {
		final long now = System.currentTimeMillis();
		final long lastModified = f.lastModified();
		if (lastModified != 0) {
			if (now - lastModified > C.IMAGE_DISC_CACHE_TOUCH_AFTER_MILLIS) {
				if (!f.setLastModified(now)) LOG.w("Failed to update last modified date for '%s'.", f.getAbsolutePath());
			}
		}
		else {
			LOG.w("Failed to read last modified date for '%s'.", f.getAbsolutePath());
		}
	}

	public static void cleanCacheDir (final Context context) {
		final File dir = getBaseDir(context);
		long bytesFreed = 0L;
		if (dir.exists()) {
			final long now = System.currentTimeMillis();
			for (final File f : dir.listFiles()) {
				if (now - f.lastModified() > C.IMAGE_DISC_CACHE_EXPIRY_MILLIS) {
					final long fLength = f.length();
					if (f.delete()) {
						bytesFreed += fLength;
					}
					else {
						LOG.w("Failed to delete expired file: '%s'.", f.getAbsolutePath());
					}
				}
			}
		}
		LOG.i("Freed %s bytes of cached image files.", bytesFreed);
	}

	private static File getBaseDir (final Context context) {
		return new File(context.getCacheDir(), "images");
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
			catch (final IOException e) {
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
