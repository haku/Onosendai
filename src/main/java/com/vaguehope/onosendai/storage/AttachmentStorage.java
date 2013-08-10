package com.vaguehope.onosendai.storage;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.os.Environment;

import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.LogWrapper;

public final class AttachmentStorage {

	private static final String DIR_NAME = "attachments";
	private static final String EXT_TMP_PREFIX = "onosendai_";
	private static final long TMP_SCALED_IMG_EXPIRY_MILLIS = TimeUnit.DAYS.toMillis(7);
	private static final LogWrapper LOG = new LogWrapper("AS");

	private AttachmentStorage () {
		throw new AssertionError();
	}

	public static File getTempFile (final Context context, final String name) {
		return new File(getBaseDir(context), name);
	}

	public static File getExternalTempFile (final String ext) {
		return new File(Environment.getExternalStorageDirectory(),
				EXT_TMP_PREFIX + Integer.toHexString((int) System.currentTimeMillis()) + ext);
	}

	public static File moveToInternalStorage (final Context context, final File file) throws IOException {
		final File internal = getTempFile(context, file.getName());
		IoHelper.copy(file, internal);
		if (!file.delete()) throw new IOException("Failed to delete temp file '" + file.getAbsolutePath() + "'.");
		return internal;
	}

	public static void cleanTempOutputDir (final Context context) {
		final File dir = getBaseDir(context);
		long bytesFreed = 0L;
		if (dir.exists()) {
			final long now = System.currentTimeMillis();
			for (final File f : dir.listFiles()) {
				if (now - f.lastModified() > TMP_SCALED_IMG_EXPIRY_MILLIS) {
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
		LOG.i("Freed %s bytes of temp files.", bytesFreed);
	}

	private static File getBaseDir (final Context context) {
		final File baseDir = new File(context.getCacheDir(), DIR_NAME);
		if (!baseDir.exists()) baseDir.mkdirs(); // NOSONAR Not ignoring exceptional return value, result checked in next line.
		if (!baseDir.exists()) throw new IllegalStateException("Failed to create temp directory: " + baseDir.getAbsolutePath());
		return baseDir;
	}

}
