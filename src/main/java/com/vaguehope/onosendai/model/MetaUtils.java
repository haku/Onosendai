package com.vaguehope.onosendai.model;

import java.util.Collection;

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

	public static String firstMetaOfTypesData (final Collection<Meta> metas, final MetaType type) {
		for (final Meta meta : metas) {
			if (type == meta.getType()) return meta.getData();
		}
		return null;
	}

}
