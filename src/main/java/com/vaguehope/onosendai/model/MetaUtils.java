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

	public static Account accountFromMeta (final Collection<Meta> metas, final Config c) {
		final String accountMetaData = firstMetaOfTypesData(metas, MetaType.ACCOUNT);
		return accountMetaData != null ? c.getAccount(accountMetaData) : null;
	}

	public static String firstMetaOfTypesData (final Collection<Meta> metas, final MetaType type) {
		if (metas == null) return null;
		for (final Meta meta : metas) {
			if (type == meta.getType()) return meta.getData();
		}
		return null;
	}

	public static int countMetaOfType (final Collection<Meta> metas, final MetaType type) {
		int c = 0;
		for (final Meta meta : metas) {
			if (type == meta.getType()) c += 1;
		}
		return c;
	}

	public static boolean containsMetaWithTitle (final Collection<Meta> metas, final String title) {
		for (final Meta meta : metas) {
			if (title.equals(meta.getTitle())) return true;
		}
		return false;
	}

}
