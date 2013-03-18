package com.vaguehope.onosendai.payload;

public abstract class Payload {

	private final PayloadType type;

	public Payload (final PayloadType type) {
		this.type = type;
	}

	public PayloadType getType () {
		return this.type;
	}

	public abstract String getTitle ();

}
