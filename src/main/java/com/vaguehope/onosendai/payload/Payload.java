package com.vaguehope.onosendai.payload;

import android.content.Intent;

public abstract class Payload {

	private final PayloadType type;

	public Payload (final PayloadType type) {
		this.type = type;
	}

	public PayloadType getType () {
		return this.type;
	}

	public abstract String getTitle ();

	public boolean intentable () {
		return false;
	}

	public Intent toIntent () {
		throw new UnsupportedOperationException("This payload type '" + this.type + "' can not be expressed as an intent.");
	}

}
