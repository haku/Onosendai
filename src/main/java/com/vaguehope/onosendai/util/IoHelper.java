package com.vaguehope.onosendai.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public final class IoHelper {

	private static final int COPY_BUFFER_SIZE = 1024 * 4;

	private IoHelper () {
		throw new AssertionError();
	}

	public static void closeQuietly (final Closeable c) {
		if (c == null) return;
		try {
			c.close();
		}
		catch (IOException e) {/**/} // NOSONAR this is intentional, is in the name of the method.
	}

	public static long copy (final InputStream source, final OutputStream sink) throws IOException {
		byte[] buffer = new byte[COPY_BUFFER_SIZE];
		long bytesReadTotal = 0L;
		int bytesRead;
		while ((bytesRead = source.read(buffer)) != -1) {
			sink.write(buffer, 0, bytesRead);
			bytesReadTotal += bytesRead;
		}
		return bytesReadTotal;
	}

	public static String toString (final InputStream is) throws IOException {
		final StringBuilder sb = new StringBuilder();
		final BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		try {
			String line;
			while ((line = rd.readLine()) != null) {
				sb.append(line).append("\n");
			}
			return sb.toString();
		}
		finally {
			is.close();
		}
	}

	/**
	 * Returns null if file does not exist.
	 */
	public static String fileToString (final File file) throws IOException {
		try {
			final FileInputStream stream = new FileInputStream(file);
			try {
				final FileChannel fc = stream.getChannel();
				final MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
				/* Instead of using default, pass in a decoder. */
				return Charset.defaultCharset().decode(bb).toString();
			}
			finally {
				stream.close();
			}
		}
		catch (final FileNotFoundException e) {
			return null;
		}
	}

	public static void stringToFile (final String data, final File f) throws IOException {
		streamToFile(new ByteArrayInputStream(data.getBytes("UTF-8")), f);
	}

	public static void resourceToFile (final String res, final File f) throws IOException {
		final InputStream is = IoHelper.class.getResourceAsStream(res);
		try {
			streamToFile(is, f);
		}
		finally {
			closeQuietly(is);
		}
	}

	public static void streamToFile (final InputStream is, final File f) throws IOException {
		final OutputStream os = new FileOutputStream(f);
		try {
			copy(is, os);
		}
		finally {
			closeQuietly(os);
		}
	}

}
