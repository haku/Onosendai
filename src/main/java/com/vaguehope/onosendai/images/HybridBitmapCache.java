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
import com.vaguehope.onosendai.util.IoHelper.CopyProgressListener;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.SyncMgr;

public class HybridBitmapCache {

	public interface LoadListener {
		void onContentLengthToLoad (long contentLength);
		void onContentLengthToFetch (long contentLength);
		void onContentFetching (int bytesFetched, int contentLength);
	}

	private static final int BASE_HEX = 16;
	static final LogWrapper LOG = new LogWrapper("HC");

	private final Context context;
	private final MemoryBitmapCache<String> memCache;
	private final File baseDir;
	private final SyncMgr syncMgr;
	private final Bitmap errorBitmap;

	public HybridBitmapCache (final Context context, final int maxMemorySizeBytes) {
		this.context = context;
		this.memCache = new MemoryBitmapCache<String>(maxMemorySizeBytes);
		this.baseDir = getBaseDir(context);
		if (!this.baseDir.exists() && !this.baseDir.mkdirs()) throw new IllegalStateException("Failed to create cache directory: " + this.baseDir.getAbsolutePath());
		this.syncMgr = new SyncMgr();
		this.errorBitmap = BitmapFactory.decodeResource(this.context.getResources(), R.drawable.exclamation_red);
		LOG.i("in memory cache: %s bytes.", maxMemorySizeBytes);
	}

	public Bitmap quickGet (final String key) {
		return this.memCache.get(key);
	}

	/**
	 * @return null if image is not in cache.
	 * @throws UnrederableException
	 *             if image is in cache but can not be rendered.
	 */
	public Bitmap get (final String key, final int reqWidth, final LoadListener listener) throws UnrederableException {
		Bitmap bmp = this.memCache.get(key);
		if (bmp == null) bmp = getFromDisc(key, reqWidth, listener);
		return bmp;
	}

	public HttpStreamHandler<Bitmap> fromHttp (final String key, final int reqWidth, final LoadListener listener) {
		return new DiscCacheHandler(this, key, reqWidth, listener);
	}

	public void clean () {
		this.memCache.evictAll();
	}

	protected Bitmap getFromDisc (final String key, final int reqWidth, final LoadListener listener) throws UnrederableException {
		if (key == null) return null;
		final File file = keyToFile(key);
		if (!file.exists()) return null;
		if (listener != null) listener.onContentLengthToLoad(file.length());
		final Bitmap bmp = decodeBitmap(file, reqWidth);
		if (bmp == null) {
			cacheFailureInMemory(key);
			throw new UnrederableException(file);
		}
		this.memCache.put(key, bmp);
		refreshFileTimestamp(file);
		return bmp;
	}

	protected void cacheFailureInMemory (final String key) {
		this.memCache.put(key, this.errorBitmap);
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
			if (now - lastModified > C.IMAGE_DISC_CACHE_TOUCH_AFTER_MILLIS && !f.setLastModified(now)) {
				LOG.w("Failed to update last modified date for '%s'.", f.getAbsolutePath());
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

	private static Bitmap decodeBitmap (final File file, final int reqWidth) {
		if (reqWidth < 1) return BitmapFactory.decodeFile(file.getAbsolutePath());

		final BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
		final int srcWidth = opts.outWidth;

		opts.inSampleSize = calculateInSampleSize(srcWidth, reqWidth);
		//LOG.i("Decoding '%s' reqWidth=%s srcWidth=%s sampleSize=%s.", file.getAbsolutePath(), reqWidth, srcWidth, opts.inSampleSize);
		opts.inJustDecodeBounds = false;
		try {
			return BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
		}
		catch (final OutOfMemoryError e) {
			final int oldSampleSize = opts.inSampleSize;
			opts.inSampleSize *= 2;
			LOG.w("OOM decoding '%s' reqWidth=%s srcWidth=%s sampleSize=%s, retrying with sampleSize=%s...",
					file.getAbsolutePath(), reqWidth, srcWidth, oldSampleSize, opts.inSampleSize);
			return BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
		}
	}

	private static int calculateInSampleSize (final int srcWidth, final int reqWidth) {
		int inSampleSize = 1;
		if (srcWidth > reqWidth) {
			while ((srcWidth / 2) / inSampleSize >= reqWidth) {
				inSampleSize *= 2;
			}
		}
		return inSampleSize;
	}

	private static class DiscCacheHandler implements HttpStreamHandler<Bitmap> {

		private final HybridBitmapCache cache;
		private final String key;
		private final int reqWidth;
		private final LoadListener listener;

		public DiscCacheHandler (final HybridBitmapCache cache, final String key, final int reqWidth, final LoadListener listener) {
			this.cache = cache;
			this.key = key;
			this.reqWidth = reqWidth;
			this.listener = listener;
		}

		@Override
		public void onError (final Exception e) {
			this.cache.cacheFailureInMemory(this.key);
		}

		@Override
		public Bitmap handleStream (final InputStream is, final int contentLength) throws IOException {
			if (this.listener != null) this.listener.onContentLengthToFetch(contentLength);
			final File f = this.cache.keyToFile(this.key);
			final OutputStream os = new FileOutputStream(f);
			try {
				final long bytesCopied;
				if (this.listener != null) {
					bytesCopied = IoHelper.copyWithProgress(is, os, new FetchProgressListener(contentLength, this.listener));
				}
				else {
					bytesCopied = IoHelper.copy(is, os);
				}
				if (bytesCopied < 1L) throw new IOException(String.format("%s bytes returned.", bytesCopied));
			}
			catch (final IOException e) {
				if (!f.delete()) LOG.e("Failed to delete incomplete download '" + f.getAbsolutePath() + "': " + e.toString());
				throw e;
			}
			finally {
				os.close();
			}
			return this.cache.getFromDisc(this.key, this.reqWidth, this.listener);
		}

	}

	private static class FetchProgressListener implements CopyProgressListener {

		private final int contentLength;
		private final int updateStep;
		private final LoadListener listener;
		private int lastUpdateBytesCopied = 0;

		public FetchProgressListener (final int contentLength, final LoadListener listener) {
			this.contentLength = contentLength;
			this.updateStep = contentLength / 100;
			this.listener = listener;
		}

		@Override
		public void onCopyProgress (final int bytesCopied) {
			if (this.contentLength < 1) return;
			if (bytesCopied - this.lastUpdateBytesCopied >= this.updateStep) {
				this.lastUpdateBytesCopied = bytesCopied;
				this.listener.onContentFetching(bytesCopied, this.contentLength);
			}
		}

	}

}
