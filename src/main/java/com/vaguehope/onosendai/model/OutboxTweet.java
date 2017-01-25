package com.vaguehope.onosendai.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.net.Uri;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.provider.ServiceRef;
import com.vaguehope.onosendai.util.ArrayHelper;
import com.vaguehope.onosendai.util.StringHelper;
import com.vaguehope.onosendai.util.Titleable;

public class OutboxTweet {

	public enum OutboxAction implements Titleable {
		POST(0, "Post", "Posting"), //ES
		RT(1, "RT", "RTing"), //ES
		DELETE(2, "Delete", "Deleting"), //ES
		FAV(3, "Favourite", "Favouriting"); //ES

		private final int code;
		private final String title;
		private final String verb;

		private OutboxAction (final int code, final String title, final String verb) {
			this.code = code;
			this.title = title;
			this.verb = verb;
		}

		@Override
		public String getUiTitle () {
			return this.title;
		}

		public int getCode () {
			return this.code;
		}

		public String getUiVerb () {
			return this.verb;
		}

		public static OutboxAction parseCode (final Integer code) {
			if (code == null) throw new IllegalArgumentException("Code can not be null.");
			switch (code) {
				case 0:
					return OutboxAction.POST;
				case 1:
					return OutboxAction.RT;
				case 2:
					return OutboxAction.DELETE;
				case 3:
					return OutboxAction.FAV;
				default:
					throw new IllegalArgumentException("Code can not be null.");
			}
		}
	}

	public enum OutboxTweetStatus {
		UNKNOWN(0, "Unknown"), //ES
		PENDING(1, "Pending"), //ES
		PERMANENTLY_FAILED(2, "Failed"), //ES
		PAUSED(3, "Draft"), //ES
		SENT(4, "Sent"); //ES

		private final int code;
		private final String name;

		private OutboxTweetStatus (final int code, final String name) {
			this.code = code;
			this.name = name;
		}

		public int getCode () {
			return this.code;
		}

		@Override
		public String toString () {
			return this.name;
		}

		public static OutboxTweetStatus parseCode (final Integer code) {
			if (code == null) return UNKNOWN;
			switch (code) {
				case 0:
					return UNKNOWN;
				case 1:
					return PENDING;
				case 2:
					return PERMANENTLY_FAILED;
				case 3:
					return PAUSED;
				case 4:
					return SENT;
				default:
					return UNKNOWN;
			}
		}
	}

	/**
	 * _id from DB.
	 */
	private final Long uid;

	private final OutboxAction action;
	private final String accountId;
	private final List<String> svcMetas;
	private final String body;
	private final String inReplyToSid;
	private final Uri attachment;
	private final OutboxTweetStatus status;
	private final Long statusTime;
	private final Integer attemptCount;
	private final String lastError;
	private final String sid;

	/**
	 * Initial.
	 */
	public OutboxTweet (final OutboxAction action, final Account account, final Set<ServiceRef> svcs, final String body, final String inReplyToSid, final Uri attachment) {
		this(null, action, account.getId(), svcsToList(svcs), body, inReplyToSid, attachment, OutboxTweetStatus.UNKNOWN, null, 0, null, null);
	}

	/**
	 * From DB.
	 */
	public OutboxTweet (final long uid, final OutboxAction action, final String accountId, final String svcMetas, final String body, final String inReplyToSid, final String attachment,
			final Integer statusCode, final Long statusTime, final Integer attemptCount, final String lastError,
			final String sid) {
		this(uid, action, accountId, svcsStrToList(svcMetas), body, inReplyToSid, safeParseUri(attachment),
				OutboxTweetStatus.parseCode(statusCode), statusTime, attemptCount, lastError, sid);
	}

	/**
	 * Add error details.
	 */
	private OutboxTweet (final OutboxTweet ot, final OutboxTweetStatus status, final Long statusTime, final Integer attemptCount, final String lastError) {
		this(ot.getUid(), ot.getAction(), ot.getAccountId(), ot.getSvcMetasList(), ot.getBody(), ot.getInReplyToSid(), ot.getAttachment(),
				status, statusTime, attemptCount, lastError, ot.getSid());
	}

	/**
	 * Mark as sent.
	 */
	private OutboxTweet (final OutboxTweet ot, final OutboxTweetStatus status, final Long statusTime, final String lastError, final String sid) {
		this(ot.getUid(), ot.getAction(), ot.getAccountId(), ot.getSvcMetasList(), ot.getBody(), ot.getInReplyToSid(), ot.getAttachment(),
				status, statusTime, ot.getAttemptCount(), lastError, sid);
	}

	/**
	 * Set uid after edit.
	 */
	private OutboxTweet (final OutboxTweet ot, final Long uid) {
		this(uid, ot.getAction(), ot.getAccountId(), ot.getSvcMetasList(), ot.getBody(), ot.getInReplyToSid(), ot.getAttachment(),
				ot.getStatus(), ot.getStatusTime(), ot.getAttemptCount(), ot.getLastError(), ot.getSid());
		if (ot.getUid() != null) throw new IllegalArgumentException(String.format("Can not set uid=%s as already have uid=%s.", uid, ot.getUid()));
	}

	/**
	 * Set inReplyToSid when replying to outbox entry.
	 */
	private OutboxTweet (final OutboxTweet ot, final String inReplyToSid) {
		this(ot.getUid(), ot.getAction(), ot.getAccountId(), ot.getSvcMetasList(), ot.getBody(), inReplyToSid, ot.getAttachment(),
				ot.getStatus(), ot.getStatusTime(), ot.getAttemptCount(), ot.getLastError(), ot.getSid());
	}

	/**
	 * Set attachment after moving internal.
	 */
	private OutboxTweet (final OutboxTweet ot, final Uri attachment) {
		this(ot.getUid(), ot.getAction(), ot.getAccountId(), ot.getSvcMetasList(), ot.getBody(), ot.getInReplyToSid(), attachment,
				ot.getStatus(), ot.getStatusTime(), ot.getAttemptCount(), ot.getLastError(), ot.getSid());
	}

	private OutboxTweet (final Long uid, final OutboxAction action, final String accountId, final List<String> svcMetas, final String body, final String inReplyToSid, final Uri attachment,
			final OutboxTweetStatus status, final Long statusTime, final Integer attemptCount, final String lastError,
			final String sid) {
		this.uid = uid;
		this.action = action;
		this.accountId = accountId;
		this.svcMetas = Collections.unmodifiableList(svcMetas);
		this.body = body;
		this.inReplyToSid = inReplyToSid;
		this.attachment = attachment;
		this.status = status;
		this.statusTime = statusTime;
		this.attemptCount = attemptCount;
		this.lastError = lastError;
		this.sid = sid;
	}

	public Long getUid () {
		return this.uid;
	}

	public OutboxAction getAction () {
		return this.action;
	}

	public String getAccountId () {
		return this.accountId;
	}

	public List<String> getSvcMetasList () {
		return this.svcMetas;
	}

	public String getSvcMetasStr () {
		return svcsListToStr(this.svcMetas);
	}

	public Set<ServiceRef> getSvcMetasParsed () {
		return svcsListToParsed(getSvcMetasList());
	}

	public String getBody () {
		return this.body;
	}

	public String getInReplyToSid () {
		return this.inReplyToSid;
	}

	public Uri getAttachment () {
		return this.attachment;
	}

	public String getAttachmentStr () {
		return this.attachment != null ? this.attachment.toString() : null;
	}

	public OutboxTweetStatus getStatus () {
		return this.status;
	}

	public Integer getStatusCode () {
		if (this.status == null) return null;
		return this.status.getCode();
	}

	/**
	 * Time current status was set.
	 */
	public Long getStatusTime () {
		return this.statusTime;
	}

	public int getAttemptCount () {
		if (this.attemptCount == null) return 0;
		return this.attemptCount.intValue();
	}

	public String getLastError () {
		return this.lastError;
	}

	public String getSid () {
		return this.sid;
	}

	public OutboxTweet permFailure (final String error) {
		return new OutboxTweet(this, OutboxTweetStatus.PERMANENTLY_FAILED, System.currentTimeMillis(), getAttemptCount() + 1, error);
	}

	public OutboxTweet tempFailure (final String error) {
		return new OutboxTweet(this, OutboxTweetStatus.PENDING, System.currentTimeMillis(), getAttemptCount() + 1, error);
	}

	public OutboxTweet setPending () {
		return new OutboxTweet(this, OutboxTweetStatus.PENDING, System.currentTimeMillis(), getAttemptCount(), getLastError());
	}

	public OutboxTweet setPaused () {
		return new OutboxTweet(this, OutboxTweetStatus.PAUSED, System.currentTimeMillis(), getAttemptCount(), getLastError());
	}

	public OutboxTweet markAsSent (final String sid) {
		return new OutboxTweet(this, OutboxTweetStatus.SENT, System.currentTimeMillis(), "", sid);
	}

	public OutboxTweet withUid (final long newUid) {
		return new OutboxTweet(this, newUid);
	}

	public OutboxTweet withInReplyToSid (final String newInReplyToSid) {
		return new OutboxTweet(this, newInReplyToSid);
	}

	public OutboxTweet withAttachment (final Uri attachment) {
		return new OutboxTweet(this, attachment);
	}

	@Override
	public String toString () {
		return new StringBuilder()
				.append("OutboxTweet{").append(this.uid)
				.append(",").append(this.action)
				.append(",").append(this.accountId)
				.append(",").append(this.getSvcMetasStr())
				.append(",").append(this.body)
				.append(",").append(this.inReplyToSid)
				.append(",").append(this.getAttachmentStr())
				.append(",").append(this.getLastError())
				.append("}").toString();
	}

	public Tweet toTweet (final Config conf) {
		final Account account = conf.getAccount(this.accountId);
		return new TweetBuilder()
				.body(this.body)
				.fullname(account != null ? account.getUiTitle() : "(unknown account: " + this.accountId + ")")
				.id(this.sid)
				.build();
		// TODO add Metas?
	}

	private static final String TEMP_SID_PREFIX = "outbox:";

	public String getTempSid () {
		if (this.uid == null) throw new IllegalStateException("UID is not set.");
		return TEMP_SID_PREFIX + this.uid;
	}

	public static boolean isTempSid (final String sid) {
		if (sid == null) return false;
		return sid.startsWith(TEMP_SID_PREFIX);
	}

	public static long uidFromTempSid (final String sid) {
		if (!isTempSid(sid)) throw new IllegalArgumentException("Not a temp sid: " + sid);
		try {
			return Long.parseLong(sid.substring(TEMP_SID_PREFIX.length()));
		}
		catch (final NumberFormatException e) {
			throw new IllegalArgumentException("Invalid temp sid: " + sid, e);
		}
	}

	private static Uri safeParseUri (final String s) {
		if (s == null) return null;
		return Uri.parse(s);
	}

	private static List<String> svcsToList (final Set<ServiceRef> svcs) {
		final List<String> l = new ArrayList<String>();
		if (svcs != null) {
			for (final ServiceRef svc : svcs) {
				l.add(svc.toServiceMeta());
			}
		}
		return l;
	}

	private static String svcsListToStr (final List<String> svcMetasList) {
		return ArrayHelper.join(svcMetasList, "|");
	}

	private static List<String> svcsStrToList (final String svcMetasStr) {
		if (StringHelper.isEmpty(svcMetasStr)) return Collections.emptyList();
		return Arrays.asList(svcMetasStr.split("\\|"));
	}

	private static Set<ServiceRef> svcsListToParsed (final List<String> svcList) {
		final Set<ServiceRef> ret = new HashSet<ServiceRef>();
		for (final String meta : svcList) {
			ret.add(ServiceRef.parseServiceMeta(meta));
		}
		return ret;
	}

}
