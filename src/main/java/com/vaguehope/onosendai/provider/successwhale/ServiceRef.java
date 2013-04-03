package com.vaguehope.onosendai.provider.successwhale;

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

	@Override
	public String toString () {
		return new StringBuilder()
				.append("ServiceRef{").append(this.rawType)
				.append(",").append(this.uid)
				.append("}").toString();
	}

}
