package com.vaguehope.onosendai.update;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import android.content.Context;
import android.database.Cursor;

import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.storage.CachedStringGroup;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.storage.TweetCursorReader;
import com.vaguehope.onosendai.ui.pref.FetchingPrefFragment;
import com.vaguehope.onosendai.util.LogWrapper;

public class FetchLinkService extends AbstractBgFetch {

	protected static final LogWrapper LOG = new LogWrapper("FLS");

	public static void startServiceIfConfigured (final Context context, final Prefs prefs, final Collection<Column> columns, final boolean manual) {
		AbstractBgFetch.startServiceIfConfigured(FetchLinkService.class, FetchingPrefFragment.KEY_PREFETCH_LINKS, context, prefs, columns, manual);
	}

	public FetchLinkService () {
		super(FetchLinkService.class, LOG);
	}

	@Override
	protected void readUrls (final Cursor cursor, final TweetCursorReader reader, final List<String> urls) {
		final List<Meta> metas = getDb().getTweetMetasOfType(reader.readUid(cursor), MetaType.URL);
		if (metas != null) {
			for (final Meta m : metas) {
				if (m.getData() != null) urls.add(m.getData());
			}
		}
	}

	@Override
	protected void makeJobs (final List<String> urls, final Map<String, Callable<?>> jobs) {
		final DbInterface db = getDb();
		for (final String url : urls) {
			if (db.cachedString(CachedStringGroup.LINK_TITLE, url) == null
					&& db.cachedString(CachedStringGroup.LINK_DEST_URL, url) == null) {
				jobs.put(url, new FetchLinkTitle(db, url));
			}
		}
	}

}
