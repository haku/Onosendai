package com.vaguehope.onosendai.storage;

public enum CachedStringGroup {
	LINK_TITLE(1),
	LINK_DEST_URL(2);

	private final int id;

	private CachedStringGroup (final int id) {
		this.id = id;
	}

	public int getId () {
		return this.id;
	}

}
