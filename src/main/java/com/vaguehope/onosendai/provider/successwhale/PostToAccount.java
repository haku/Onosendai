package com.vaguehope.onosendai.provider.successwhale;

public class PostToAccount {

	private final String service;
	private final String username;
	private final String uid;
	private final boolean enabled;

	public PostToAccount (final String service, final String username, final String uid, final boolean enabled) {
		this.service = service;
		this.username = username;
		this.uid = uid;
		this.enabled = enabled;
	}

	public String getService () {
		return this.service;
	}

	public String getUsername () {
		return this.username;
	}

	public String getUid () {
		return this.uid;
	}

	public boolean isEnabled () {
		return this.enabled;
	}

	public String getDisplayName () {
		if (this.username != null && !this.username.isEmpty()) return this.username;
		if (this.uid != null && !this.uid.isEmpty()) return this.uid;
		if (this.service != null && !this.service.isEmpty()) return this.service;
		return "(unknown)";
	}

}
