package com.vaguehope.onosendai.model;

public enum MetaType {

	/**
	 * Data is an image URL that can be shown as a preview.
	 * Title may be the click URL.
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
	REPLYTO(8),
	/**
	 * Data is the real time the Tweet was posted if Tweet.getTime() has been overwritten.
	 * Time is in seconds (utime).
	 * Title is unused.
	 */
	POST_TIME(9),
	/**
	 * The SID to use when making changes to this post, such as deleting it.
	 * e.g. Deleting a RT uses the RT id (this field) not the SID of the RT'ed tweet.
	 * Title is unused.
	 */
	EDIT_SID(10),
	/**
	 * This tweet has been successfully deleted.
	 * Data is the utime in seconds of when it was deleted.
	 * Title is unused.
	 */
	DELETED(11),
	/**
	 * Used to hint which resource item was fetched from.
	 * Data is ColumnFeed.resourceHash().
	 * Title is unused.
	 */
	FEED_HASH(12),
	/**
	 * This is hack that will never be in the DB.
	 * This is to attach the source column ID to search results.
	 */
	COLUMN_ID(90);

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
			case 9: // NOSONAR 9 is not magical.
				return POST_TIME;
			case 10: // NOSONAR 10 is not magical.
				return EDIT_SID;
			case 11: // NOSONAR 11 is not magical.
				return DELETED;
			case 12: // NOSONAR 12 is not magical.
				return FEED_HASH;
			case 90:// NOSONAR 90 is not that magical.
				return COLUMN_ID;
			default:
				return null;
		}
	}

}
