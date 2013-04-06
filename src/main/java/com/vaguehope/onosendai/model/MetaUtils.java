package com.vaguehope.onosendai.model;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Config;


public final class MetaUtils {

	private MetaUtils () {
		throw new AssertionError();
	}

	public static Account accountFromMeta(final Tweet t, final Config c) {
		final Meta accountMeta = t.getFirstMetaOfType(MetaType.ACCOUNT);
		return accountMeta != null ? c.getAccount(accountMeta.getData()) : null;
	}

}
