package com.vaguehope.onosendai.update;

import java.util.concurrent.Callable;

import android.text.Spanned;

import com.vaguehope.onosendai.storage.CachedStringGroup;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.HtmlTitleParser;
import com.vaguehope.onosendai.util.HttpHelper;
import com.vaguehope.onosendai.util.HttpHelper.FinalUrlHandler;

public class FetchLinkTitle implements Callable<Void> {

	private final DbInterface db;
	private final String url;

	public FetchLinkTitle (final DbInterface db, final String url) {
		if (db == null) throw new IllegalArgumentException("db can not be null.");
		if (url == null) throw new IllegalArgumentException("url can not be null.");
		this.db = db;
		this.url = url;
	}

	@Override
	public Void call () throws Exception {
		if (this.db.cachedString(CachedStringGroup.LINK_TITLE, this.url) == null
				&& this.db.cachedString(CachedStringGroup.LINK_DEST_URL, this.url) == null) {
			final FinalUrlHandler<Spanned> handler = new FinalUrlHandler<Spanned>(HtmlTitleParser.INSTANCE);
			final CharSequence title = HttpHelper.get(this.url, handler);
			if (title != null) {
				this.db.cacheString(CachedStringGroup.LINK_TITLE, this.url, title.toString());
			}
			this.db.cacheString(CachedStringGroup.LINK_DEST_URL, this.url, handler.getUrl().toString());
		}
		return null;
	}

}
