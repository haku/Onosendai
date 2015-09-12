package com.vaguehope.onosendai.update;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import android.text.Spanned;

import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.provider.twitter.TwitterUrls;
import com.vaguehope.onosendai.storage.CachedStringGroup;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.HtmlTitleParser;
import com.vaguehope.onosendai.util.HttpHelper;
import com.vaguehope.onosendai.util.HttpHelper.FinalUrlHandler;
import com.vaguehope.onosendai.util.LogWrapper;

public class FetchLinkTitle implements Callable<Void> {

	public interface FetchTitleListener {
		void onLinkTitle (Meta m, String title, URL finalUrl) throws IOException;
	}

	private static final Pattern UNTITLEABLE_URL = Pattern.compile("^.*\\.(mp[0-9]|m3u8|mov|webm|mpd|jpe?g|png|gif|pdf)$", Pattern.CASE_INSENSITIVE);
	private static final LogWrapper LOG = new LogWrapper("FLT");

	public static boolean shouldFetchTitle (final Meta m) {
		return m.getData() != null &&
				TwitterUrls.readTweetSidFromUrl(m.getData()) == null &&
				!UNTITLEABLE_URL.matcher(m.getData()).matches();
	}

	public static boolean isTitleCached (final DbInterface db, final Meta meta) {
		return db.cachedString(CachedStringGroup.LINK_DEST_URL, meta.getData()) != null
				|| db.cachedString(CachedStringGroup.LINK_TITLE, meta.getData()) != null;
	}

	public static void fetchTitle (final DbInterface db, final Meta m, final FetchTitleListener l) throws IOException {
		final String cachedTitle = db.cachedString(CachedStringGroup.LINK_TITLE, m.getData());
		final String cachedDestUrl = db.cachedString(CachedStringGroup.LINK_DEST_URL, m.getData());
		if (cachedTitle != null || cachedDestUrl != null) {
			if (l != null) l.onLinkTitle(m, cachedTitle, cachedDestUrl != null ? new URL(cachedDestUrl) : null);
		}
		else {
			final FinalUrlHandler<Spanned> handler = new FinalUrlHandler<Spanned>(HtmlTitleParser.INSTANCE);
			final CharSequence title = HttpHelper.get(m.getData(), handler);
			final String finalUrl = handler.getUrl().toString();
			LOG.i("%s: '%s' %s.", m.getData(), title, finalUrl);
			if (l != null) l.onLinkTitle(m, title != null ? title.toString() : null, handler.getUrl());
			if (title != null) {
				db.cacheString(CachedStringGroup.LINK_TITLE, m.getData(), title.toString());
			}
			db.cacheString(CachedStringGroup.LINK_DEST_URL, m.getData(), finalUrl);
		}

	}

	private final DbInterface db;
	private final Meta meta;

	public FetchLinkTitle (final DbInterface db, final Meta meta) {
		if (db == null) throw new IllegalArgumentException("db can not be null.");
		if (meta == null) throw new IllegalArgumentException("meta can not be null.");
		if (meta.getData() == null) throw new IllegalArgumentException("meta.data can not be null.");
		this.db = db;
		this.meta = meta;
	}

	@Override
	public Void call () throws Exception {
		fetchTitle(this.db, this.meta, null);
		return null;
	}

}
