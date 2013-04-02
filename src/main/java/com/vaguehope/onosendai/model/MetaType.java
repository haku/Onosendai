package com.vaguehope.onosendai.model;

public enum MetaType {

	MEDIA(1),
	HASHTAG(2),
	MENTION(3),
	URL(4),
	INREPLYTO(5),
	SERVICE(6),
	ACCOUNT(7);

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
			case 3: // NOSONAR 3 is not magical.
				return MENTION;
			case 4: // NOSONAR 4 is not magical.
				return URL;
			case 5: // NOSONAR 5 is not magical.
				return INREPLYTO;
			case 6: // NOSONAR 6 is not magical.
				return SERVICE;
			case 7: // NOSONAR 7 is not magical.
				return ACCOUNT;
			default:
				return null;
		}
	}

}
