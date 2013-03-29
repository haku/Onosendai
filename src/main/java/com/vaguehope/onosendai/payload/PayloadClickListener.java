package com.vaguehope.onosendai.payload;

public interface PayloadClickListener {

	/**
	 * Return true if handled.
	 */
	boolean payloadClicked (Payload payload);

	void subviewClicked (Payload payload, int index);

}
