package com.vaguehope.onosendai.config;

public class ConfigException extends Exception {

	private static final long serialVersionUID = 5229290894847058366L;

	public ConfigException (final String s) {
		super(s);
	}

	public ConfigException (final Exception e) {
		super(e.getMessage(), e);
	}

}
