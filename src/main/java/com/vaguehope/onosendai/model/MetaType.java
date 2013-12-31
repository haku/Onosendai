package com.vaguehope.onosendai.model;

public enum MetaType {

	/**
	 * Data is an image URL that can be shown as a preview.
	 */
	MEDIA(1),
	/**
	 * Data is the hashtag without the preceding "#".
	 */
	HASHTAG(2),
	/**
	 * Data is the screenname without the preceding "@".
	 * Title may be the full name.
	 */
	MENTION(3),
	/**
	 * Data is the full URL, title is the friendly presentation version.
	 */
	URL(4),
	/**
	 * The Service provided ID (SID) of the tweet that this tweet is a reply to.
	 */
	INREPLYTO(5),
	/**
	 * SuccessWhale sub-account type.
	 */
	SERVICE(6),
	/**
	 * Account (as appears in deck.conf).
	 */
	ACCOUNT(7),
	/**
	 * The Service provided ID (SID) to reply to if different from the item SID.
	 * e.g. Facebook notifications.
	 */
	REPLYTO(8);

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
			case 8: // NOSONAR 8 is not magical.
				return REPLYTO;
			default:
				return null;
		}
	}

}
