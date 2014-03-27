package com.vaguehope.onosendai.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore.MediaColumns;

public class ImageMetadata implements Titleable {

	private static final String SCHEME_FILE = "file";
	private static final String SCHEME_CONTENT = "content";
	private static final LogWrapper LOG = new LogWrapper("IM");

	private final Context context;
	private final Uri uri;
	private final long size;
	private final String name;

	private final Object[] bitmapLock = new Object[0];
	private volatile AssetFileDescriptor fileDescriptor;
	private volatile SoftReference<Bitmap> cacheBitmap;
	private volatile int cacheScale = -1;

	private volatile int cachedWidth = -1;
	private volatile int cachedHeight = -1;

	public ImageMetadata (final Context context, final Uri uri) {
		this.context = context;
		this.uri = uri;
		if (uri == null) {
			this.size = 0;
			this.name = null;
		}
		else if (SCHEME_CONTENT.equals(uri.getScheme())) {
			final Cursor cursor = context.getContentResolver().query(uri, new String[] {
					MediaColumns.SIZE, MediaColumns.DISPLAY_NAME
			}, null, null, null);
			try {
				if (cursor != null) {
					cursor.moveToFirst();
					final int colSize = cursor.getColumnIndex(MediaColumns.SIZE);
					final int colDisplayName = cursor.getColumnIndex(MediaColumns.DISPLAY_NAME); // Filename with extension.
					this.size = cursor.getLong(colSize);
					this.name = cursor.getString(colDisplayName);
				}
				else {
					throw new IllegalArgumentException("Resource not found: " + uri);
				}
			}
			finally {
				IoHelper.closeQuietly(cursor);
			}
		}
		else if (SCHEME_FILE.equals(uri.getScheme())) {
			this.size = new File(uri.getPath()).length();
			this.name = uri.getLastPathSegment();
		}
		else {
			throw new IllegalArgumentException("Unknown resource type: " + uri);
		}
	}

	public boolean exists () {
		return this.uri != null;
	}

	/**
	 * With file extension.
	 */
	public String getName () {
		return this.name;
	}

	public long getSize () {
		return this.size;
	}

	@Override
	public String getUiTitle () {
		if (this.name == null) return "(empty)";
		return String.format("%s (%s)", this.name, IoHelper.readableFileSize(this.size));
	}

	public InputStream open () throws IOException {
		if (this.uri == null) return null;
		if (SCHEME_FILE.equals(this.uri.getScheme())) return new FileInputStream(new File(this.uri.getPath()));
		if (SCHEME_CONTENT.equals(this.uri.getScheme())) return this.context.getContentResolver().openInputStream(this.uri);
		throw new IllegalArgumentException("Unknown resource type: " + this.uri);
	}

	private AssetFileDescriptor openFileDescriptor () throws IOException {
		if (this.uri == null) return null;
		synchronized (this.bitmapLock) {
			if (this.fileDescriptor == null) {
				if (SCHEME_FILE.equals(this.uri.getScheme()) || SCHEME_CONTENT.equals(this.uri.getScheme())) {
					this.fileDescriptor = this.context.getContentResolver().openAssetFileDescriptor(this.uri, "r");
				}
				else {
					throw new IllegalArgumentException("Unknown resource type: " + this.uri);
				}
			}
			return this.fileDescriptor;
		}
	}

	public int getWidth () throws IOException {
		if (this.cachedWidth < 1) readDimentions();
		return this.cachedWidth;
	}

	public int getHeight () throws IOException {
		if (this.cachedHeight < 1) readDimentions();
		return this.cachedHeight;
	}

	private void readDimentions () throws IOException {
		synchronized (this.bitmapLock) {
			final Options opts = new Options();
			opts.inJustDecodeBounds = true;
			BitmapFactory.decodeFileDescriptor(openFileDescriptor().getFileDescriptor(), null, opts);
			this.cachedWidth = opts.outWidth;
			this.cachedHeight = opts.outHeight;
		}
	}

	public Bitmap getBitmap (final int scalePercentage) throws IOException {
		return getBitmap(scalePercentage, null);
	}

	public Bitmap getBitmap (final int scalePercentage, final Rect cropRect) throws IOException {
		synchronized (this.bitmapLock) {
			final Bitmap cached = this.cacheBitmap == null ? null : this.cacheBitmap.get();

			if (cached != null && scalePercentage != this.cacheScale) {
				this.cacheBitmap = null;
				cached.recycle();
			}

			if (cached == null || scalePercentage != this.cacheScale) {
				final Bitmap fresh = readBitmap(scalePercentage, cropRect);
				this.cacheScale = scalePercentage;
				this.cacheBitmap = new SoftReference<Bitmap>(fresh);
				return fresh;
			}
			return cached;
		}
	}

	private Bitmap readBitmap (final int scalePercentage, final Rect cropRect) throws IOException {
		if (100 % scalePercentage != 0) throw new IllegalArgumentException("scalePercentage " + scalePercentage + " is not a int ratio.");
		final Options opts = new Options();
		opts.inPurgeable = true;
		opts.inInputShareable = true;
		opts.inSampleSize = 100 / scalePercentage;

		if (cropRect != null) {
			final BitmapRegionDecoder dec = BitmapRegionDecoder.newInstance(openFileDescriptor().getFileDescriptor(), true);
			try {
				return dec.decodeRegion(cropRect, opts);
			}
			finally {
				dec.recycle();
			}
		}

		return BitmapFactory.decodeFileDescriptor(openFileDescriptor().getFileDescriptor(), null, opts);
	}

	public void recycle () {
		synchronized (this.bitmapLock) {
			if (this.cacheBitmap != null) {
				final Bitmap cached = this.cacheBitmap.get();
				if (cached != null) cached.recycle();
				this.cacheBitmap = null;
			}
			if (this.fileDescriptor != null) {
				try {
					this.fileDescriptor.close();
				}
				catch (final IOException e) {
					LOG.w("Failed to dispose of fileDescriptor.", e);
				}
				this.fileDescriptor = null;
			}
		}
	}

	@Override
	public String toString () {
		return new StringBuilder(getClass().getSimpleName()).append("{").append(this.uri)
				.append(",").append(this.name)
				.append(",").append(this.size)
				.append("}").toString();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static boolean isUnderstoodResource (final Uri uri) {
		if (uri == null) return false;
		return SCHEME_CONTENT.equals(uri.getScheme()) || SCHEME_FILE.equals(uri.getScheme());
	}

}
