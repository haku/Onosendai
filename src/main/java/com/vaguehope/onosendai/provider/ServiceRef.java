package com.vaguehope.onosendai.provider;

import java.util.Arrays;
import java.util.Collection;

import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleProvider;
import com.vaguehope.onosendai.util.EqualHelper;

public class ServiceRef {

	private final String id;
	private final String rawServiceType;
	private final String uid;
	private final String username;
	private final boolean enabled;

	private NetworkType serviceType;

	public ServiceRef (final String rawRype, final String uid) {
		this(null, rawRype, uid, null, false);
	}

	public ServiceRef (final String rawServiceType, final String uid, final String username, final boolean enabled) {
		this(null, rawServiceType, uid, username, enabled);
	}

	public ServiceRef (final String id, final String rawServiceType, final String uid, final String username, final boolean enabled) {
		this.id = id;
		this.rawServiceType = rawServiceType;
		this.uid = uid;
		this.username = username;
		this.enabled = enabled;
	}

	/**
	 * The new ID assigned to this account by the provider.
	 */
	public String getId () {
		return this.id;
	}

	public String getRawType () {
		return this.rawServiceType;
	}

	public NetworkType getType () {
		if (this.serviceType == null) this.serviceType = NetworkType.parse(this.rawServiceType);
		return this.serviceType;
	}

	/**
	 * The ID for the service where the content ends up.
	 */
	public String getUid () {
		return this.uid;
	}

	public String getUsername () {
		return this.username;
	}

	public boolean isEnabled () {
		return this.enabled;
	}

	public String toServiceMeta () {
		return SuccessWhaleProvider.createServiceMeta(this.rawServiceType, this.uid);
	}

	public String getDisplayName () {
		if (this.username != null && !this.username.isEmpty()) return this.username;
		if (this.uid != null && !this.uid.isEmpty()) return this.uid;
		if (this.rawServiceType != null && !this.rawServiceType.isEmpty()) return this.rawServiceType;
		return "(unknown)";
	}

	@Override
	public String toString () {
		return new StringBuilder()
				.append("ServiceRef{").append(this.id)
				.append(",").append(this.rawServiceType)
				.append(",").append(this.uid)
				.append(",").append(this.username)
				.append(",").append(this.enabled)
				.append("}").toString();
	}

	@Override
	public int hashCode () {
		return Arrays.hashCode(new Object[] { this.id, this.rawServiceType, this.uid, this.username, this.enabled });
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof ServiceRef)) return false;
		ServiceRef that = (ServiceRef) o;
		return EqualHelper.equal(this.id, that.id)
				&& EqualHelper.equal(this.rawServiceType, that.rawServiceType)
				&& EqualHelper.equal(this.uid, that.uid)
				&& EqualHelper.equal(this.username, that.username)
				&& this.enabled == that.enabled;
	}

	public static String humanList (final Collection<ServiceRef> col, final String token) {
		if (col == null) return null;
		if (col.size() < 1) return "";
		StringBuilder b = new StringBuilder();
		for (ServiceRef s : col) {
			if (b.length() > 0) b.append(token);
			b.append(s.rawServiceType).append(":").append(s.uid);
		}
		return b.toString();
	}

}
