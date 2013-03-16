package com.vaguehope.onosendai.provider.successwhale;


public class SuccessWhaleException extends Exception {

	private static final long serialVersionUID = -2004908955108746560L;

	public SuccessWhaleException (final String msg) {
		super(msg);
	}

	public SuccessWhaleException (final String msg, final Exception e) {
		super(msg, e);
	}

}
