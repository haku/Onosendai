package com.vaguehope.onosendai.provider.successwhale;

import java.io.IOException;

import org.apache.http.HttpResponse;

class InvalidRequestException extends HttpRequestException {

	private static final long serialVersionUID = 8386398554192783582L;

	public InvalidRequestException (final HttpResponse response) throws IOException {
		super(response);
	}

}
