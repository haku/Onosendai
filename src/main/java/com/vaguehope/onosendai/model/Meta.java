package com.vaguehope.onosendai.model;

public class Meta {

	private final MetaType type;
	private final String data;

	public Meta (final MetaType type, final String data) {
		this.type = type;
		this.data = data;
	}

	public MetaType getType () {
		return this.type;
	}

	public String getData () {
		return this.data;
	}

}
