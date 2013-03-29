package com.vaguehope.onosendai.payload;

import java.util.ArrayList;
import java.util.List;

public class PayloadList {

	private final List<Payload> payloads;

	public PayloadList (final List<Payload> payloads) {
		this.payloads = new ArrayList<Payload>(payloads);
	}

	void addItem(final Payload payload) {
		this.payloads.add(payload);
	}

	void replaceItem(final Payload find, final Payload with) {
		for (int i = 0; i < this.payloads.size(); i ++) {
			if (find.equals(this.payloads.get(i))) this.payloads.set(i, with);
		}
	}

	void removeItem (final Payload payload) {
		this.payloads.remove(payload);
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

	@Override
	public String toString () {
		return new StringBuilder("PayloadList{")
				.append(this.payloads)
				.append("}").toString();
	}

}
