package com.vaguehope.onosendai.config;

public class ConfigUnavailableException extends ConfigException {

	private static final long serialVersionUID = -990987433735620215L;

	public ConfigUnavailableException (final String s) {
		super(s);
	}

	public ConfigUnavailableException (final Exception e) {
		super(e);
	}

}
