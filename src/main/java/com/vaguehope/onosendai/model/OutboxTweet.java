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

public class OutboxTweet {

	private final Long uid;
	private final String accountId;
	private final List<String> svcMetas;
	private final String body;
	private final String inReplyToSid;
	private final Uri attachment;
	private final String lastError;

	public OutboxTweet (final Account account, final Set<ServiceRef> svcs, final String body, final String inReplyToSid, final Uri attachment) {
		this(null, account.getId(), svcsToList(svcs), body, inReplyToSid, attachment, null);
	}

	public OutboxTweet (final Long uid, final String accountId, final String svcMetas, final String body, final String inReplyToSid, final String attachment, final String lastError) {
		this(uid, accountId, svcsStrToList(svcMetas), body, inReplyToSid, safeParseUri(attachment), lastError);
	}

	public OutboxTweet (final OutboxTweet ot, final String lastError) {
		this(ot.getUid(), ot.getAccountId(), ot.getSvcMetasList(), ot.getBody(), ot.getInReplyToSid(), ot.getAttachment(), lastError);
	}

	private OutboxTweet (final Long uid, final String accountId, final List<String> svcMetas, final String body, final String inReplyToSid, final Uri attachment, final String lastError) {
		this.uid = uid;
		this.accountId = accountId;
		this.svcMetas = Collections.unmodifiableList(svcMetas);
		this.body = body;
		this.inReplyToSid = inReplyToSid;
		this.attachment = attachment;
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
		return Arrays.asList(svcMetasStr.split("\\|"));
	}

	private static Set<ServiceRef> svcsListToParsed (final List<String> svcList) {
		final Set<ServiceRef> ret = new HashSet<ServiceRef>();
		for (String meta : svcList) {
			ret.add(ServiceRef.parseServiceMeta(meta));
		}
		return ret;
	}

	public String getLastError () {
		return this.lastError;
	}

}
