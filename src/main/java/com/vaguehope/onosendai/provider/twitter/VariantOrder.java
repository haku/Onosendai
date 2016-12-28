package com.vaguehope.onosendai.provider.twitter;

import java.util.Comparator;

import twitter4j.MediaEntity.Variant;

public enum VariantOrder implements Comparator<Variant> {
	INSTANCE;

	@Override
	public int compare (final Variant lhs, final Variant rhs) {
		if (lhs == null) return rhs == null ? 0 : 1;
		if (rhs == null) return -1;
		final int a = rhs.getBitrate() - lhs.getBitrate();
		if (a != 0) return a;
		if (lhs.getContentType() == null) return rhs.getContentType() == null ? 0 : 1;
		if (rhs.getContentType() == null) return -1;
		return lhs.getContentType().compareTo(rhs.getContentType());
	}

}
