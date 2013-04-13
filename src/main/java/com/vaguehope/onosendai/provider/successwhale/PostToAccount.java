package com.vaguehope.onosendai.provider.successwhale;

import java.util.Arrays;

import com.vaguehope.onosendai.util.EqualHelper;

public class PostToAccount {

	private final String id;
	private final String service;
	private final String username;
	private final String uid;
	private final boolean enabled;

	public PostToAccount (final String service, final String username, final String uid, final boolean enabled) {
		this(null, service, username, uid, enabled);
	}

	public PostToAccount (final String id, final String service, final String username, final String uid, final boolean enabled) {
		this.id = id;
		this.service = service;
		this.username = username;
		this.uid = uid;
		this.enabled = enabled;
	}

	/**
	 * The new ID assigned to this account by the provider.
	 */
	public String getId () {
		return this.id;
	}

	public String getService () {
		return this.service;
	}

	public String getUsername () {
		return this.username;
	}

	/**
	 * The ID for the service where the content ends up.
	 */
	public String getUid () {
		return this.uid;
	}

	public boolean isEnabled () {
		return this.enabled;
	}

	public ServiceRef toSeviceRef () {
		return new ServiceRef(this.id, this.service, this.uid);
	}

	public String getDisplayName () {
		if (this.username != null && !this.username.isEmpty()) return this.username;
		if (this.uid != null && !this.uid.isEmpty()) return this.uid;
		if (this.service != null && !this.service.isEmpty()) return this.service;
		return "(unknown)";
	}

	@Override
	public String toString () {
		return new StringBuilder("PostToAccount{").append(this.service)
				.append(",").append(this.username)
				.append(",").append(this.uid)
				.append(",").append(this.enabled)
				.append("}").toString();
	}

	@Override
	public int hashCode () {
		return Arrays.hashCode(new Object[] { this.service, this.username, this.uid, this.enabled });
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof PostToAccount)) return false;
		PostToAccount that = (PostToAccount) o;
		return EqualHelper.equal(this.service, that.service)
				&& EqualHelper.equal(this.username, that.username)
				&& EqualHelper.equal(this.uid, that.uid)
				&& this.enabled == that.enabled;
	}

}
