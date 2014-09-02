package com.vaguehope.onosendai.payload;

import android.view.View;

public interface PayloadClickListener {

	/**
	 * Return true if handled.
	 */
	boolean payloadClicked (Payload payload);

	void subviewClicked (View v, Payload payload, int index);

}
