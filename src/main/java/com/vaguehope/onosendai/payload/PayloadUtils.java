package com.vaguehope.onosendai.payload;

import java.util.ArrayList;
import java.util.List;

import com.vaguehope.onosendai.model.Tweet;


public final class PayloadUtils {

	private PayloadUtils () {
		throw new AssertionError();
	}

	public static PayloadList extractPayload (final Tweet tweet) {
		List<Payload> ret = new ArrayList<Payload>();

		return new PayloadList(ret);
	}

}
