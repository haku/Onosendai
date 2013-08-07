package com.vaguehope.onosendai.provider.successwhale;

import java.io.IOException;

class NotAuthorizedException extends IOException {

	private static final long serialVersionUID = -7925922593362766365L;

	public NotAuthorizedException () {
		super("HTTP 401");
	}

}
