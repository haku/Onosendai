package com.vaguehope.onosendai.images;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.ExcpetionHelper;
import com.vaguehope.onosendai.util.HashHelper;
import com.vaguehope.onosendai.util.HttpHelper.HttpStreamHandler;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.IoHelper.CopyProgressListener;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.StringHelper;
import com.vaguehope.onosendai.util.SyncMgr;

public class HybridBitmapCache {

	private static final String META_EXT = ".txt";

	public interface LoadListener {
		void onContentLengthToLoad (long contentLength);
		void onContentLengthToFetch (long contentLength);
		void onContentFetching (int bytesFetched, int contentLength);
	}

	private static final int BASE_HEX = 16;
	static final LogWrapper LOG = new LogWrapper("HC");

	private final MemoryBitmapCache<String> memCache;
	private final int maxMemCacheEntrySize;
	private final LruCache<String, String> failuresCache;
	private final File baseDir;
	private final SyncMgr syncMgr = new SyncMgr();
	private final ImageLoadRequestManager reqMgr = new ImageLoadRequestManager();

	public HybridBitmapCache (final Context context, final int maxMemorySizeBytes) {
		this.memCache = maxMemorySizeBytes > 0 ? new MemoryBitmapCache<String>(maxMemorySizeBytes) : null;
		this.failuresCache = new LruCache<String, String>(100); // TODO extract constant.
		this.baseDir = getBaseDir(context);
		if (!this.baseDir.exists() && !this.baseDir.mkdirs()) throw new IllegalStateException("Failed to create cache directory: " + this.baseDir.getAbsolutePath());

		if (this.memCache != null) {
			this.maxMemCacheEntrySize = (int) (maxMemorySizeBytes * C.MAX_MEMORY_IMAGE_CACHE_ENTRY_SIZE_RATIO);
			LOG.i("in memory cache: total %s bytes, max entry %s bytes.", maxMemorySizeBytes, this.maxMemCacheEntrySize);
		}
		else {
			this.maxMemCacheEntrySize = 0;
		}
	}

	public void forget (final String key) throws IOException {
		if (this.memCache != null) this.memCache.remove(key);
		this.failuresCache.remove(key);
		final File file = keyToFile(key);
		if (file.exists() && !file.delete() && file.exists()) {
			throw new IOException(String.format("Failed to rm cache file %s.", file.getAbsolutePath()));
		}
	}

	public String getFailure(final String key) {
		return this.failuresCache.get(key);
	}

	public Bitmap quickGet (final String key) {
		if (this.memCache == null) return null;
		return this.memCache.get(key);
	}

	/**
	 * If a file is returned the image is cached.
	 * This does NOT refresh the file time stamp.
	 */
	public File getCachedFile (final String key) {
		if (key == null) return null;
		final File file = keyToFile(key);
		if (!file.exists()) return null;
		return file;
	}

	/**
	 * Check if the cache has this file and mark it as just been used if so.
	 * Returns true if file exists.
	 */
	public boolean touchFileIfExists (final String key) {
		final File f = getCachedFile(key);
		if (f != null) {
			refreshFileTimestamp(f);
			return true;
		}
		return false;
	}

	/**
	 * @return null if image is not in cache.
	 * @throws UnrederableException
	 *             if image is in cache but can not be rendered.
	 */
	public Bitmap get (final String key, final int reqWidth, final LoadListener listener) throws UnrederableException {
		Bitmap bmp = this.memCache != null ? this.memCache.get(key) : null;
		if (bmp == null) bmp = getFromDisc(key, reqWidth, listener);
		return bmp;
	}

	public HttpStreamHandler<Bitmap> fromHttp (final String key, final int reqWidth, final LoadListener listener) {
		return new DiscCacheHandler(this, key, reqWidth, listener);
	}

	/**
	 * This specifically does not return the decoded bitmap.
	 * It is for background prefetching.
	 * It always returns null;
	 */
	public HttpStreamHandler<?> fromHttp (final String key) {
		return new DiscCacheHandler(this, key);
	}

	public void clean () {
		if (this.memCache != null) this.memCache.evictAll();
		this.failuresCache.evictAll();
		this.reqMgr.clear();
	}

	protected Bitmap getFromDisc (final String key, final int reqWidth, final LoadListener listener) throws UnrederableException {
		if (key == null) return null;
		final File file = keyToFile(key);
		if (!file.exists()) return null;

		final long fileLength = file.length();
		if (fileLength < 1) return null;
		if (listener != null) listener.onContentLengthToLoad(fileLength);

		final Bitmap bmp = decodeBitmap(file, reqWidth);
		if (bmp == null) {
			final UnrederableException unEx = new UnrederableException(file);
			cacheFailureInMemory(key, unEx);
			throw unEx;
		}
		if (this.memCache != null && MemoryBitmapCache.bmpByteCount(bmp) <= this.maxMemCacheEntrySize) this.memCache.put(key, bmp);
		refreshFileTimestamp(file);
		return bmp;
	}

	protected void cacheFailureInMemory (final String key, final Exception ex) {
		this.failuresCache.put(key, ExcpetionHelper.veryShortMessage(ex));
	}

	protected File keyToFile (final String key) {
		return new File(this.baseDir, HashHelper.md5String(key).toString(BASE_HEX));
	}

	protected File tempFile (final String key) throws IOException {
		return File.createTempFile(HashHelper.md5String(key).toString(BASE_HEX), ".part", this.baseDir);
	}

	public SyncMgr getSyncMgr () {
		return this.syncMgr;
	}

	public ImageLoadRequestManager getReqMgr () {
		return this.reqMgr;
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

	public static void cleanCacheDir (final Context context, final DbInterface db) {
		final File dir = getBaseDir(context);
		if (!dir.exists()) return;

		final long now = System.currentTimeMillis();
		long bytesFreed = 0L;
		for (final File f : dir.listFiles()) {
			final boolean isMetaFile;
			{
				final File file = metaFileToFile(f);
				if (file != null && file.exists()) continue;
				isMetaFile = file != null;
			}

			if (now - f.lastModified() > C.IMAGE_DISC_CACHE_EXPIRY_MILLIS) {
				if(!isMetaFile && isImageInUse(f, db)) {
					refreshFileTimestamp(f);
					continue;
				}

				final long fLength = f.length();
				if (f.delete()) {
					bytesFreed += fLength;
				}
				else {
					LOG.w("Failed to delete expired file: '%s'.", f.getAbsolutePath());
				}
			}
		}
		LOG.i("Freed %s bytes of cached image files.", bytesFreed);
	}

	private static boolean isImageInUse (final File file, final DbInterface db) {
		final String key = readKeyFromMetaFile(file);
		if (!StringHelper.isEmpty(key)) {
			final List<Tweet> metas = db.findTweetsWithMeta(MetaType.MEDIA, key, 1);
			if (metas != null && metas.size() > 0) return true;

			final List<Tweet> tweets = db.findTweetsWithAvatarUrl(key, 1);
			if (tweets != null && tweets.size() > 0) return true;
		}
		return false;
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
		final int srcHeight = opts.outHeight;

		opts.inSampleSize = calculateInSampleSize(srcWidth, srcHeight, reqWidth, C.MAX_IMAGE_SIZE_BYTES);
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

	private static int calculateInSampleSize (final int srcWidth, final int srcHeight, final int reqWidth, final int maxSize) {
		int inSampleSize = 1;
		if (srcWidth > reqWidth || estimateSize(srcWidth, srcHeight, inSampleSize) > maxSize) {
			while ((srcWidth / 2) / inSampleSize >= reqWidth || estimateSize(srcWidth, srcHeight, inSampleSize) > maxSize) {
				inSampleSize *= 2;
			}
		}
		return inSampleSize;
	}

	private static int estimateSize (final int w, final int h, final int inSampleSize) {
		return (w * h * 8) / inSampleSize;
	}

	private static File fileToMetaFile (final File file) {
		if (file == null) return null;
		return new File(file.getAbsolutePath() + META_EXT);
	}

	/**
	 * Returns null if not a meta file.
	 */
	private static File metaFileToFile (final File metaFile) {
		if (metaFile == null) return null;

		final String path = metaFile.getAbsolutePath();
		if (!path.endsWith(META_EXT)) return null;

		return new File(path.substring(0, path.length() - META_EXT.length()));
	}

	protected static void writeMetaFile(final File file, final String key) throws IOException {
		final File metaFile = fileToMetaFile(file);
		IoHelper.stringToFile(key, metaFile);
	}

	public static String readKeyFromMetaFileName(final Context context, final String fileName) {
		final File file = new File(getBaseDir(context), fileName);
		return readKeyFromMetaFile(file);
	}

	/**
	 * Return null if not found or read fails.
	 */
	private static String readKeyFromMetaFile(final File file) {
		final File metaFile = fileToMetaFile(file);
		if (!metaFile.exists()) return null;

		try {
			final String meta = IoHelper.fileToString(metaFile);
			return StringHelper.firstLine(meta);
		}
		catch (final IOException e) {
			LOG.w("Failed to read meta file for %s: %s", file, ExcpetionHelper.causeTrace(e));
			return null;
		}
	}

	private static class DiscCacheHandler implements HttpStreamHandler<Bitmap> {

		private final HybridBitmapCache cache;
		private final String key;
		private final int reqWidth;
		private final LoadListener listener;
		private boolean decodeBitmap;

		public DiscCacheHandler (final HybridBitmapCache cache, final String key) {
			this(cache, key, 0, null);
			this.decodeBitmap = false;
		}

		public DiscCacheHandler (final HybridBitmapCache cache, final String key, final int reqWidth, final LoadListener listener) {
			this.cache = cache;
			this.key = key;
			this.reqWidth = reqWidth;
			this.listener = listener;
			this.decodeBitmap = true;
		}

		@Override
		public void onError (final Exception e) {
			this.cache.cacheFailureInMemory(this.key, e);
		}

		@Override
		public Bitmap handleStream (final URLConnection connection, final InputStream is, final int contentLength) throws IOException {
			if (this.listener != null) this.listener.onContentLengthToFetch(contentLength);
			final File tmpFile = this.cache.tempFile(this.key);
			final OutputStream os = new FileOutputStream(tmpFile);
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
				if (!tmpFile.delete()) LOG.e("Failed to delete incomplete download '" + tmpFile.getAbsolutePath() + "': " + e.toString());
				throw e;
			}
			finally {
				os.close();
			}

			final File file = this.cache.keyToFile(this.key);
			if (!tmpFile.renameTo(file)) throw new IOException(String.format("Failed to mv tmp file %s to %s.", tmpFile.getAbsolutePath(), file.getAbsolutePath()));

			writeMetaFile(file, this.key);

			if (this.decodeBitmap) return this.cache.getFromDisc(this.key, this.reqWidth, this.listener);
			return null;
		}

	}

	private static class FetchProgressListener implements CopyProgressListener {

		private final int contentLength;
		private final int updateStep;
		private final LoadListener listener;
		private int lastUpdateBytesCopied = 0;

		public FetchProgressListener (final int contentLength, final LoadListener listener) {
			this.contentLength = contentLength;
			this.updateStep = Math.max(contentLength / 100, 10240);
			this.listener = listener;
		}

		@Override
		public void onCopyProgress (final int bytesCopied) {
			if (bytesCopied - this.lastUpdateBytesCopied >= this.updateStep) {
				this.lastUpdateBytesCopied = bytesCopied;
				this.listener.onContentFetching(bytesCopied, this.contentLength);
			}
		}

	}

}
