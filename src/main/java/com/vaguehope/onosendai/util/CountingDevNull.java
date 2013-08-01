package com.vaguehope.onosendai.util;

import java.io.IOException;
import java.io.OutputStream;

public class CountingDevNull extends OutputStream {

	private long count = 0L;

	public long getCount () {
		return this.count;
	}

	@Override
	public void write (final byte[] b, final int off, final int len) {
		this.count += len;
	}

	@Override
	public void write (final int b) {
		this.count += 1;
	}

	@Override
	public void write (final byte[] b) throws IOException {
		this.count += b.length;
	}

}
