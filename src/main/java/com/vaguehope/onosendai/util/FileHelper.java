package com.vaguehope.onosendai.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.FileProvider;

public final class FileHelper {

	private static final Random RANDOM = new Random();

	private FileHelper () {
		throw new AssertionError();
	}

	/**
	 * This file will not already exist.
	 */
	public static File newFileInDir(final File dir, final String nameHint) {
		final String name = makeSafeName(nameHint);
		File f = new File(dir, name);
		if (!f.exists()) return f;

		final int x = name.lastIndexOf(".");
		final String baseName = x > 0 ? name.substring(0, x) : name;
		final String ext = x > 0 ? name.substring(x) : ""; // With '.'.

		while (true) {
			f = new File(dir, baseName + "." + RANDOM.nextInt(100000) + ext);
			if (!f.exists()) return f;
		}
	}

	public static String makeSafeName (final String name) {
		return name.replaceAll("[^a-zA-Z0-9\\.-]+", "_");
	}

	public static ArrayList<Uri> filesToProvidedUris (final Context context, final List<File> files) {
		final ArrayList<Uri> uris = new ArrayList<Uri>();
		for (final File file : files) {
			uris.add(FileProvider.getUriForFile(context, "com.vaguehope.onosendai.fileprovider", file));
		}
		return uris;
	}

	/**
	 * Returns null if can not determine.
	 */
	public static String nameFromPath (final String path) {
		if (StringHelper.isEmpty(path)) return null;
		final String cleanedPath = path
				.replaceFirst("^https?://", "")
				.replaceFirst("//+", "")
				.replaceFirst("^/", "")
				.replaceFirst("/$", "");
		if (StringHelper.isEmpty(cleanedPath)) return null;
		return makeSafeName(cleanedPath);
	}

	public static long fileLastModifiedAgeMillis (final File f) throws IOException {
		if (!f.exists()) return Long.MAX_VALUE;

		final long lastModified = f.lastModified();
		if (lastModified != 0) {
			return System.currentTimeMillis() - lastModified;
		}
		else {
			throw new IOException(String.format("Failed to read last modified date for '%s'.", f.getAbsolutePath()));
		}
	}

	public static void touchFile (final File f, final long graceMillis) throws IOException {
		if (!f.exists()) f.createNewFile();

		final long now = System.currentTimeMillis();
		final long lastModified = f.lastModified();
		if (lastModified != 0) {
			if (now - lastModified > graceMillis && !f.setLastModified(now)) {
				throw new IOException(String.format("Failed to update last modified date for '%s'.", f.getAbsolutePath()));
			}
		}
		else {
			throw new IOException(String.format("Failed to read last modified date for '%s'.", f.getAbsolutePath()));
		}
	}

}
