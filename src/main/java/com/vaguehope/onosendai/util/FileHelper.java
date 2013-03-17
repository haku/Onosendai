/*
 * Copyright 2011 Fae Hutter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.vaguehope.onosendai.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public final class FileHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private FileHelper () {
		throw new AssertionError();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * Returns null if file does not exist.
	 */
	public static String fileToString (final File file) throws IOException {
		try {
			FileInputStream stream = new FileInputStream(file);
			try {
				FileChannel fc = stream.getChannel();
				MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
				/* Instead of using default, pass in a decoder. */
				return Charset.defaultCharset().decode(bb).toString();
			}
			finally {
				stream.close();
			}
		}
		catch (FileNotFoundException e) {
			return null;
		}
	}

	public static void resourceToFile(final String res, final File f) throws IOException {
		byte[] arr = new byte[1024 * 4];
		InputStream is = FileHelper.class.getResourceAsStream(res);
		try {
			OutputStream os = new FileOutputStream(f);
			try {
				int count;
				while ((count = is.read(arr)) >= 0) {
					os.write(arr, 0, count);
				}
			}
			finally {
				os.close();
			}
		}
		finally {
			is.close();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
