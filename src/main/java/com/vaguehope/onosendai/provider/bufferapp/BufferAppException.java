package com.vaguehope.onosendai.provider.bufferapp;

public class BufferAppException extends Exception {

	private static final long serialVersionUID = -4639692527467133806L;

	public BufferAppException (final String msg) {
		super(msg);
	}

	public BufferAppException (final String msg, final Exception e) {
		super(msg, e);
	}

}
