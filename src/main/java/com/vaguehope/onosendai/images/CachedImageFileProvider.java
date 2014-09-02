package com.vaguehope.onosendai.images;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.v4.content.FileProvider;
import android.webkit.MimeTypeMap;

import com.vaguehope.onosendai.util.LogWrapper;

public class CachedImageFileProvider extends FileProvider {

	private static final LogWrapper LOG = new LogWrapper("CIFP");

	public CachedImageFileProvider () {}

	public static List<File> addFileExtensions (final List<File> input) {
		final List<File> ret = new ArrayList<File>();
		for (final File file : input) {
			ret.add(new File(String.format("%s.%s", file.getAbsolutePath(), identifyFileExtension(file))));
		}
		return ret;
	}

	public static String identifyFileExtension (final File file) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(file.getAbsolutePath(), options);
		return MimeTypeMap.getSingleton().getExtensionFromMimeType(options.outMimeType);
	}

	public static Uri removeExtension (final Uri uri) {
		return uri.buildUpon().path(removeExtension(uri.getPath())).build();
	}

	private static String removeExtension (final String path) {
		final int dot = path.lastIndexOf(".");
		final int slash = path.lastIndexOf("/");
		if (dot > slash && dot < path.length() - 1) return path.substring(0, dot);
		return path;
	}

	private static String getExtension (final String path) {
		final int dot = path.lastIndexOf(".");
		final int slash = path.lastIndexOf("/");
		if (dot > slash && dot < path.length() - 1) return path.substring(dot + 1);
		return path;
	}

	@Override
	public Cursor query (final Uri uri, final String[] projection, final String selection, final String[] selectionArgs, final String sortOrder) {
		LOG.i("query: %s, %s, %s, %s, %s", uri, Arrays.toString(projection), selection, selectionArgs, sortOrder);
		final MatrixCursor inputCursor = (MatrixCursor) super.query(removeExtension(uri), projection, selection, selectionArgs, sortOrder);

		// This mess to clone the single entry cursor and append file extensions to display name.
		final String dotExtension = "." + getExtension(uri.getPath());
		final String[] cols = new String[projection.length];
		final Object[] values = new Object[projection.length];
		inputCursor.moveToFirst();
		int i = 0;
		for (int colIndex = 0; colIndex < inputCursor.getColumnCount(); colIndex++) {
			final String colName = inputCursor.getColumnName(colIndex);
			if (OpenableColumns.DISPLAY_NAME.equals(colName)) {
				cols[i] = OpenableColumns.DISPLAY_NAME;
				values[i] = inputCursor.getString(colIndex) + dotExtension;
				i++;
			}
			else if (OpenableColumns.SIZE.equals(colName)) {
				cols[i] = OpenableColumns.SIZE;
				values[i] = inputCursor.getLong(colIndex);
				i++;
			}
		}
		LOG.i("result: %s, %s", Arrays.toString(cols), Arrays.toString(values));
		final MatrixCursor updatedCursor = new MatrixCursor(cols, 1);
		updatedCursor.addRow(values);
		return updatedCursor;
	}

	@Override
	public String getType (final Uri uri) {
		return super.getType(removeExtension(uri));
	}

	@Override
	public ParcelFileDescriptor openFile (final Uri uri, final String mode) throws FileNotFoundException {
		LOG.i("openFile: %s", uri);
		return super.openFile(removeExtension(uri), mode);
	}

}
