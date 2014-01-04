package com.vaguehope.onosendai.provider.successwhale;

import static org.junit.Assert.assertEquals;

import java.net.UnknownHostException;

import org.junit.Test;

public class SuccessWhaleExceptionTest {

	@Test
	public void itMakesFriendlyErrorForHostNotFound () throws Exception {
		final UnknownHostException uhe = new UnknownHostException("Unable to resolve host \"successwhale-api.herokuapp.com\": No address associated with hostname");
		final SuccessWhaleException swe = new SuccessWhaleException("Failed to fetch feed 'somefeed' from 'someurl': " + uhe.toString(), uhe);
		assertEquals("Network error: Unable to resolve host \"successwhale-api.herokuapp.com\": No address associated with hostname",
				swe.friendlyMessage());
	}

}
