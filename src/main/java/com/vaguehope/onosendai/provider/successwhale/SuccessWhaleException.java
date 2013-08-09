package com.vaguehope.onosendai.provider.successwhale;


public class SuccessWhaleException extends Exception {

	private static final long serialVersionUID = -2004908955108746560L;

	private final boolean permanent;

	public SuccessWhaleException (final String msg) {
		this(msg, false);
	}

	public SuccessWhaleException (final String msg, final boolean permanent) {
		super(msg);
		this.permanent = permanent;
	}

	public SuccessWhaleException (final String msg, final Exception e) {
		this(msg, e, false);
	}

	public SuccessWhaleException (final String msg, final Exception e, final boolean permanent) {
		super(msg, e);
		this.permanent = permanent;
	}

	public boolean isPermanent () {
		return this.permanent;
	}

}
