package com.vaguehope.onosendai.ui;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import android.os.Parcel;

/**
 * https://groups.google.com/forum/#!msg/robolectric/3KiNl-8LeVs/TJY5wdpu4QgJ
 */
@Implements(Parcel.class)
public class ShadowParcel {

	/**
	 * Only support read for now.
	 */
	@Implementation
	public static FileDescriptor openFileDescriptor (final String file, final int mode) throws FileNotFoundException {
		final File f = new File(file);
		try {
			return new FileInputStream(f).getFD();
		}
		catch (final IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
