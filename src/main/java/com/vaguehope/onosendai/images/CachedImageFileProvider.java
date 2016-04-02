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
import android.provider.BaseColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.OpenableColumns;
import android.support.v4.content.FileProvider;
import android.webkit.MimeTypeMap;

import com.vaguehope.onosendai.util.FileHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.StringHelper;

public class CachedImageFileProvider extends FileProvider {

	private static final LogWrapper LOG = new LogWrapper("CIFP");

	public CachedImageFileProvider () {}

	public static List<File> addFileExtensions (final List<File> input) {
		final List<File> ret = new ArrayList<File>();
		for (final File file : input) {
			final String extension = identifyFileExtension(file);
			if (!StringHelper.isEmpty(extension)) {
				ret.add(new File(String.format("%s.%s", file.getAbsolutePath(), extension)));
			}
			else {
				ret.add(file);
			}
		}
		return ret;
	}

	protected static String identifyFileExtension (final File file) {
		return MimeTypeMap.getSingleton().getExtensionFromMimeType(identifyFileMimeType(file));
	}

	protected static String identifyFileMimeType (final File file) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(file.getAbsolutePath(), options);
		return options.outMimeType;
	}

	protected static Uri removeExtension (final Uri uri) {
		return uri.buildUpon().path(removeExtension(uri.getPath())).build();
	}

	protected static String baseName (final Uri uri) {
		final List<String> segs = uri.getPathSegments();
		return segs.get(segs.size() - 1);
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
		return "";
	}

	protected static long tryReadId (final Uri uri) {
		String baseName = baseName(uri);
		if (baseName.length() > 14) baseName = baseName.substring(baseName.length() - 14);
		try {
			return Long.valueOf(baseName, 16);
		}
		catch (final NumberFormatException e) {
			LOG.w("Failed to read ID from uri=%s baseName=%s.", uri, baseName);
			return -1;
		}
	}

	@Override
	public Cursor query (final Uri uri, final String[] projection, final String selection, final String[] selectionArgs, final String sortOrder) {
		LOG.i("query: %s, %s, %s, %s, %s", uri, Arrays.toString(projection), selection, selectionArgs, sortOrder);

		final Uri uriMinusExtension = removeExtension(uri);
		final MatrixCursor inputCursor = (MatrixCursor) super.query(uriMinusExtension, projection, selection, selectionArgs, sortOrder);

		// This mess to clone the single entry cursor and append file extensions to display name.
		final List<String> cols = new ArrayList<String>();
		final List<Object> values = new ArrayList<Object>();
		inputCursor.moveToFirst();
		for (final String colName : projection) {
			final int colIndex = inputCursor.getColumnIndex(colName);
			if (OpenableColumns.DISPLAY_NAME.equals(colName)) {
				if (colIndex >= 0) {
					cols.add(OpenableColumns.DISPLAY_NAME);
					values.add(makeDisplayName(inputCursor.getString(colIndex), uri));
				}
			}
			else if (OpenableColumns.SIZE.equals(colName)) {
				if (colIndex >= 0) {
					cols.add(OpenableColumns.SIZE);
					values.add(inputCursor.getLong(colIndex));
				}
			}
			else if (MediaColumns.MIME_TYPE.equals(colName)) {
				cols.add(MediaColumns.MIME_TYPE);
				values.add(getType(uri));
			}
			else if (BaseColumns._ID.equals(colName)) {
				final long id = tryReadId(uriMinusExtension);
				if (id > 0) {
					cols.add(BaseColumns._ID);
					values.add(id);
				}
			}
		}
		final String[] colsArr = cols.toArray(new String[cols.size()]);
		LOG.i("result: %s, %s", Arrays.toString(colsArr), values);
		final MatrixCursor updatedCursor = new MatrixCursor(colsArr, 1);
		updatedCursor.addRow(values);
		return updatedCursor;
	}

	private String makeDisplayName (final String baseName, final Uri uri) {
		final String extension = getExtension(uri.getPath());

		final String key = HybridBitmapCache.readKeyFromMetaFileName(getContext(), baseName(removeExtension(uri)));
		if (!StringHelper.isEmpty(key)) {
			final String nameFromKey = FileHelper.baseNameFromPath(key);
			if (!StringHelper.isEmpty(nameFromKey)) {
				return StringHelper.addSuffexIfCaseInsensitiveMissing(nameFromKey, "." + extension);
			}
		}

		return String.format("%s.%s", baseName, extension);
	}

	@Override
	public String getType (final Uri uri) {
		// Cheat as file does not have to exist for super class to identify by extension.
		final String type = super.getType(uri);
		LOG.i("getType(%s)=%s", uri, type);
		return type;
	}

	@Override
	public ParcelFileDescriptor openFile (final Uri uri, final String mode) throws FileNotFoundException {
		LOG.i("openFile: %s", uri);
		return super.openFile(removeExtension(uri), mode);
	}

}
