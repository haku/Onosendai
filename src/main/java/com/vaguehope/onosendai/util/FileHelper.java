package com.vaguehope.onosendai.util;

import java.io.File;
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

}
