package com.vaguehope.onosendai.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.vaguehope.onosendai.C;

public final class LogcatHelper {

	private LogcatHelper () {
		throw new AssertionError();
	}

	public static void dumpLog (final File file) throws IOException, InterruptedException {
		final List<String> args = new ArrayList<String>();
		args.add("logcat");
		args.add("-b");
		args.add("main");
		args.add("-t");
		args.add("20000");
		args.add("-f");
		args.add(file.getAbsolutePath());
		args.add("-v");
		args.add("time");
		args.add("-s");
		args.add(C.TAG + ":I");

		final Process proc = Runtime.getRuntime().exec(args.toArray(new String[args.size()]));
		eatStream(proc.getInputStream());
		eatStream(proc.getErrorStream());
		final int code = proc.waitFor();

		if (code != 0) throw new IOException("Logcat exit code: " + code);
		if (!file.exists()) throw new IOException("Logcat did not write file: " + file.getAbsolutePath());
	}

	private static void eatStream (final InputStream is) {
		new Thread(new Runnable() {
			@Override
			public void run () {
				try {
					try {
						final byte[] dummy = new byte[1024];
						while (is.read(dummy) >= 0) {/* Unwanted. */} // NOSONAR do not care about content.
					}
					catch (final IOException e) {/* Unwanted. */}
				}
				finally {
					IoHelper.closeQuietly(is);
				}
			}
		}).start();
	}

}
