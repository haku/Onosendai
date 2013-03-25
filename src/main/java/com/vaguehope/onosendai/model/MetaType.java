package com.vaguehope.onosendai.model;

public enum MetaType {

	MEDIA(1),
	HASHTAG(2),
	MENTION(3);

	private final int id;

	private MetaType (final int id) {
		this.id = id;
	}

	public int getId () {
		return this.id;
	}

	public static MetaType parseId (final int id) {
		switch (id) {
			case 1:
				return MEDIA;
			case 2:
				return HASHTAG;
			case 3:
				return MENTION;
			default:
				return null;
		}
	}

}
