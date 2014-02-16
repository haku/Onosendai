package com.vaguehope.onosendai.provider;

import java.util.Arrays;
import java.util.Collection;

import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.util.EqualHelper;
import com.vaguehope.onosendai.util.Titleable;

public class ServiceRef implements Titleable {

	private final String id;
	private final String rawServiceType;
	private final String uid;
	private final String username;
	private final boolean defult;

	private NetworkType serviceType;

	public ServiceRef (final String rawServiceType, final String uid) {
		this(null, rawServiceType, uid, null, false);
	}

	public ServiceRef (final String rawServiceType, final String uid, final String username, final boolean defult) {
		this(null, rawServiceType, uid, username, defult);
	}

	public ServiceRef (final String id, final String rawServiceType, final String uid, final String username, final boolean defult) {
		this.id = id;
		this.rawServiceType = rawServiceType;
		this.uid = uid;
		this.username = username;
		this.defult = defult;
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

	public boolean isDefault () {
		return this.defult;
	}

	public String toServiceMeta () {
		return createServiceMeta(this.rawServiceType, this.uid);
	}

	@Override
	public String getUiTitle () {
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
				.append(",").append(this.defult)
				.append("}").toString();
	}

	@Override
	public int hashCode () {
		return Arrays.hashCode(new Object[] { this.id, this.rawServiceType, this.uid });
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof ServiceRef)) return false;
		final ServiceRef that = (ServiceRef) o;
		return EqualHelper.equal(this.id, that.id)
				&& EqualHelper.equal(this.rawServiceType, that.rawServiceType)
				&& EqualHelper.equal(this.uid, that.uid);
	}

	public static String humanList (final Collection<ServiceRef> col, final String token) {
		if (col == null) return null;
		if (col.size() < 1) return "";
		final StringBuilder b = new StringBuilder();
		for (final ServiceRef s : col) {
			if (b.length() > 0) b.append(token);
			b.append(s.rawServiceType).append(":").append(s.uid);
		}
		return b.toString();
	}

	public static String createServiceMeta (final String serviceType, final String serviceUid) {
		return String.format("%s:%s", serviceType, serviceUid);
	}

	public static ServiceRef parseServiceMeta (final Meta meta) {
		if (meta.getType() != MetaType.SERVICE) return null;
		return parseServiceMeta(meta.getData());
	}

	public static ServiceRef parseServiceMeta (final String meta) {
		if (meta == null) return null;
		final int x = meta.indexOf(':');
		if (x >= 0) {
			final String type = meta.substring(0, x);
			final String uid = meta.substring(x + 1);
			return new ServiceRef(type, uid);
		}
		return null;
	}

}
