package com.vaguehope.onosendai.payload;

import java.util.Collections;
import java.util.List;

public class PayloadList {

	private final List<Payload> payloads;

	public PayloadList (final List<Payload> payloads) {
		this.payloads = Collections.unmodifiableList(payloads);
	}

	public int size () {
		return this.payloads.size();
	}

	public Payload getPayload (final int index) {
		return this.payloads.get(index);
	}

	public List<Payload> getPayloads () {
		return this.payloads;
	}

}
