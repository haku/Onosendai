package com.vaguehope.onosendai.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.MediaColumns;

public class ImageMetadata implements Titleable {

	private static final String SCHEME_FILE = "file";
	private static final String SCHEME_CONTENT = "content";

	private final Context context;
	private final Uri uri;
	private final long size;
	private final String name;

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
					int colSize = cursor.getColumnIndex(MediaColumns.SIZE);
					int colDisplayName = cursor.getColumnIndex(MediaColumns.DISPLAY_NAME); // Filename with extension.
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