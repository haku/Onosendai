package com.vaguehope.onosendai.util;

import android.content.ContextWrapper;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.MediaColumns;

public final class MediaHelper {

	private MediaHelper () {
		throw new AssertionError();
	}

	public static ImageMetadata getImageMetadata (final ContextWrapper context, final Uri uri) {
		final Cursor cursor = context.getContentResolver().query(uri, new String[] {
				MediaColumns.DATA, MediaColumns.SIZE, MediaColumns.MIME_TYPE, MediaColumns.DISPLAY_NAME, MediaColumns.TITLE
		}, null, null, null);
		try {
			if (cursor == null) return null;
			cursor.moveToFirst();
			int colData = cursor.getColumnIndex(MediaColumns.DATA);
			int colSize = cursor.getColumnIndex(MediaColumns.SIZE);
			int colMimeType = cursor.getColumnIndex(MediaColumns.MIME_TYPE);
			int colDisplayName = cursor.getColumnIndex(MediaColumns.DISPLAY_NAME); // Filename with extension.
			int colTitle = cursor.getColumnIndex(MediaColumns.TITLE);              // Filename without extension.
			String filePath = cursor.getString(colData);
			long size = cursor.getLong(colSize);
			String mimeType = cursor.getString(colMimeType);
			String displayName = cursor.getString(colDisplayName);
			String title = cursor.getString(colTitle);
			return new ImageMetadata(filePath, size, mimeType, displayName, title);
		}
		finally {
			IoHelper.closeQuietly(cursor);
		}
	}

	public static class ImageMetadata {

		private final String filePath;
		private final long size;
		private final String mimeType;
		private final String displayName;
		private final String title;

		public ImageMetadata (final String filePath, final long size, final String mimeType, final String displayName, final String title) {
			this.filePath = filePath;
			this.size = size;
			this.mimeType = mimeType;
			this.displayName = displayName;
			this.title = title;
		}

		@Override
		public String toString () {
			return "ImageMetadata [filePath=" + this.filePath + ", size=" + this.size + ", mimeType=" + this.mimeType + ", displayName=" + this.displayName + ", title=" + this.title + "]";
		}

	}

}
