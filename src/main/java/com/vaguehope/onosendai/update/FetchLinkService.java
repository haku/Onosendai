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
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.storage.TweetCursorReader;
import com.vaguehope.onosendai.ui.pref.FetchingPrefFragment;
import com.vaguehope.onosendai.util.LogWrapper;

public class FetchLinkService extends AbstractBgFetch {

	private static final LogWrapper LOG = new LogWrapper("FLS");

	public static void startServiceIfConfigured (final Context context, final Prefs prefs, final Collection<Column> columns, final boolean manual) {
		AbstractBgFetch.startServiceIfConfigured(FetchLinkService.class, FetchingPrefFragment.KEY_PREFETCH_LINKS, context, prefs, columns, manual);
	}

	public FetchLinkService () {
		super(FetchLinkService.class, false, LOG);
	}

	@Override
	protected void readUrls (final Cursor cursor, final TweetCursorReader reader, final List<Meta> retMetas) {
		addMetas(getDb().getTweetMetasOfType(reader.readUid(cursor), MetaType.URL), retMetas);
	}

	@Override
	protected void readUrls (final Tweet tweet, final List<Meta> retMetas) {
		addMetas(tweet.getMetas(), retMetas);
	}

	private void addMetas (final List<Meta> tms, final List<Meta> retMetas) {
		if (tms != null) {
			for (final Meta m : tms) {
				if (FetchLinkTitle.shouldFetchTitle(m)) retMetas.add(m);
			}
		}
	}

	@Override
	protected void makeJobs (final List<Meta> metas, final Map<String, Callable<?>> jobs) {
		final DbInterface db = getDb();
		for (final Meta meta : metas) {
			if (!FetchLinkTitle.isTitleCached(db, meta)) jobs.put(meta.getData(), new FetchLinkTitle(db, meta));
		}
	}

}
