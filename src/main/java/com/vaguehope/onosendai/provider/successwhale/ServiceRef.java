package com.vaguehope.onosendai.provider.successwhale;

import java.util.Arrays;
import java.util.Collection;

import com.vaguehope.onosendai.provider.NetworkType;
import com.vaguehope.onosendai.util.EqualHelper;

public class ServiceRef {

	private final String rawType;
	private final String uid;
	private NetworkType type;

	public ServiceRef (final String rawRype, final String uid) {
		this.rawType = rawRype;
		this.uid = uid;
	}

	public String getRawType () {
		return this.rawType;
	}

	public NetworkType getType () {
		if (this.type == null) this.type = NetworkType.parse(this.rawType);
		return this.type;
	}

	public String getUid () {
		return this.uid;
	}

	public boolean matchesPostToAccount (final PostToAccount pta) {
		if (pta == null) return false;
		return EqualHelper.equal(pta.getService(), this.rawType) && EqualHelper.equal(pta.getUid(), this.uid);
	}

	public String toServiceMeta () {
		return SuccessWhaleProvider.createServiceMeta(this.rawType, this.uid);
	}

	@Override
	public String toString () {
		return new StringBuilder()
				.append("ServiceRef{").append(this.rawType)
				.append(",").append(this.uid)
				.append("}").toString();
	}

	@Override
	public int hashCode () {
		return Arrays.hashCode(new Object[] { this.rawType, this.uid });
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof ServiceRef)) return false;
		ServiceRef that = (ServiceRef) o;
		return EqualHelper.equal(this.rawType, that.rawType)
				&& EqualHelper.equal(this.uid, that.uid);
	}

	public static String humanList (final Collection<ServiceRef> col, final String token) {
		if (col == null) return null;
		if (col.size() < 1) return "";
		StringBuilder b = new StringBuilder();
		for (ServiceRef s : col) {
			if (b.length() > 0) b.append(token);
			b.append(s.rawType).append(":").append(s.uid);
		}
		return b.toString();
	}

}
