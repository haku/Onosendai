package com.vaguehope.onosendai.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.FileProvider;

public final class FileHelper {

	private FileHelper () {
		throw new AssertionError();
	}

	public static ArrayList<Uri> filesToProvidedUris (final Context context, final List<File> files) {
		final ArrayList<Uri> uris = new ArrayList<Uri>();
		for (final File file : files) {
			uris.add(FileProvider.getUriForFile(context, "com.vaguehope.onosendai.fileprovider", file));
		}
		return uris;
	}

}
