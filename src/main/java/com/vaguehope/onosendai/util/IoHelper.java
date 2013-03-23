package com.vaguehope.onosendai.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class IoHelper {

	private static final int COPY_BUFFER_SIZE = 1024 * 4;

	private IoHelper () {
		throw new AssertionError();
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

}
