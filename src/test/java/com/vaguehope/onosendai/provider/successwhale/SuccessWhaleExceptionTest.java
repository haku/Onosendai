package com.vaguehope.onosendai.provider.successwhale;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SuccessWhaleExceptionTest {

	@Test
	public void itMakesFriendlyErrorForHostNotFound () throws Exception {
		final UnknownHostException uhe = new UnknownHostException("Unable to resolve host \"successwhale-api.herokuapp.com\": No address associated with hostname");
		final SuccessWhaleException swe = new SuccessWhaleException("Failed to fetch feed 'somefeed' from 'someurl': " + uhe.toString(), uhe);
		assertEquals("Network error: Unable to resolve host \"successwhale-api.herokuapp.com\": No address associated with hostname",
				swe.friendlyMessage());
	}

	@Test
	public void itMakesFriendlyMsgForInvalidServerSideOauthToken () throws Exception {
		final HttpResponse resp = mockHttpResponse(500, "Internal Server Error",
				"<feed>" +
						"<success type=\"boolean\">false</success>" +
						"<error>type: OAuthException, code: 190, error_subcode: 460, message: Error validating access token: The session has been invalidated because the user has changed the password. [HTTP 400]</error>" +
						"<errorclass type=\"Class\">Koala::Facebook::AuthenticationError</errorclass>" +
						"</feed>");
		final SuccessWhaleException swe = new SuccessWhaleException(resp);

		assertEquals("SuccessWhale error: Please use web UI to authorise access to Facebook.",
				swe.friendlyMessage());
	}

	private static HttpResponse mockHttpResponse (final int code, final String statusMsg, final String body) throws IOException {
		final StatusLine statusLine = mock(StatusLine.class);
		when(statusLine.getStatusCode()).thenReturn(code);
		when(statusLine.getReasonPhrase()).thenReturn(statusMsg);

		final HttpEntity entity = mock(HttpEntity.class);
		when(entity.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes()));

		final HttpResponse resp = mock(HttpResponse.class);
		when(resp.getStatusLine()).thenReturn(statusLine);
		when(resp.getEntity()).thenReturn(entity);
		return resp;
	}

}
