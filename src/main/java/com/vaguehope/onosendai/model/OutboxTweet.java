package com.vaguehope.onosendai.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.net.Uri;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.provider.ServiceRef;
import com.vaguehope.onosendai.util.ArrayHelper;
import com.vaguehope.onosendai.util.StringHelper;

public class OutboxTweet {

	public enum OutboxTweetStatus {
		UNKNOWN(0, "Unknown"),
		PENDING(1, "Pending"),
		PERMANENTLY_FAILED(2, "Failed");

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
				default:
					return UNKNOWN;
			}
		}
	}

	private final Long uid;
	private final String accountId;
	private final List<String> svcMetas;
	private final String body;
	private final String inReplyToSid;
	private final Uri attachment;
	private final OutboxTweetStatus status;
	private final Integer attemptCount;
	private final String lastError;

	/**
	 * Initial.
	 */
	public OutboxTweet (final Account account, final Set<ServiceRef> svcs, final String body, final String inReplyToSid, final Uri attachment) {
		this(null, account.getId(), svcsToList(svcs), body, inReplyToSid, attachment, OutboxTweetStatus.PENDING, 0, null);
	}

	/**
	 * From DB.
	 */
	public OutboxTweet (final Long uid, final String accountId, final String svcMetas, final String body, final String inReplyToSid, final String attachment,
			final Integer statusCode, final Integer attemptCount, final String lastError) {
		this(uid, accountId, svcsStrToList(svcMetas), body, inReplyToSid, safeParseUri(attachment),
				OutboxTweetStatus.parseCode(statusCode), attemptCount, lastError);
	}

	/**
	 * Add error details.
	 */
	private OutboxTweet (final OutboxTweet ot, final OutboxTweetStatus status, final Integer attemptCount, final String lastError) {
		this(ot.getUid(), ot.getAccountId(), ot.getSvcMetasList(), ot.getBody(), ot.getInReplyToSid(), ot.getAttachment(),
				status, attemptCount, lastError);
	}

	private OutboxTweet (final Long uid, final String accountId, final List<String> svcMetas, final String body, final String inReplyToSid, final Uri attachment,
			final OutboxTweetStatus status, final Integer attemptCount, final String lastError) {
		this.uid = uid;
		this.accountId = accountId;
		this.svcMetas = Collections.unmodifiableList(svcMetas);
		this.body = body;
		this.inReplyToSid = inReplyToSid;
		this.attachment = attachment;
		this.status = status;
		this.attemptCount = attemptCount;
		this.lastError = lastError;
	}

	public Long getUid () {
		return this.uid;
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

	public int getAttemptCount () {
		if (this.attemptCount == null) return 0;
		return this.attemptCount.intValue();
	}

	public String getLastError () {
		return this.lastError;
	}

	public OutboxTweet permFailure (final String error) {
		return new OutboxTweet(this, OutboxTweetStatus.PERMANENTLY_FAILED, getAttemptCount() + 1, error);
	}

	public OutboxTweet tempFailure (final String error) {
		return new OutboxTweet(this, OutboxTweetStatus.PENDING, getAttemptCount() + 1, error);
	}

	public OutboxTweet resetToPending () {
		return new OutboxTweet(this, OutboxTweetStatus.PENDING, getAttemptCount(), getLastError());
	}

	@Override
	public String toString () {
		return new StringBuilder()
				.append("OutboxTweet{").append(this.uid)
				.append(",").append(this.accountId)
				.append(",").append(this.getSvcMetasStr())
				.append(",").append(this.body)
				.append(",").append(this.inReplyToSid)
				.append(",").append(this.getAttachmentStr())
				.append(",").append(this.getLastError())
				.append("}").toString();
	}

	private static Uri safeParseUri (final String s) {
		if (s == null) return null;
		return Uri.parse(s);
	}

	private static List<String> svcsToList (final Set<ServiceRef> svcs) {
		final List<String> l = new ArrayList<String>();
		for (final ServiceRef svc : svcs) {
			l.add(svc.toServiceMeta());
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
		for (String meta : svcList) {
			ret.add(ServiceRef.parseServiceMeta(meta));
		}
		return ret;
	}

}
