package com.vaguehope.onosendai.update;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import android.content.Context;
import android.database.Cursor;

import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.images.HybridBitmapCache;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.storage.TweetCursorReader;
import com.vaguehope.onosendai.ui.pref.FetchingPrefFragment;
import com.vaguehope.onosendai.util.LogWrapper;

public class FetchPictureService extends AbstractBgFetch {

	protected static final LogWrapper LOG = new LogWrapper("FPS");

	public static void startServiceIfConfigured (final Context context, final Prefs prefs, final Collection<Column> columns, final boolean manual) {
		AbstractBgFetch.startServiceIfConfigured(FetchPictureService.class, FetchingPrefFragment.KEY_PREFETCH_MEDIA, context, prefs, columns, manual);
	}

	public FetchPictureService () {
		super(FetchPictureService.class, LOG);
	}

	@Override
	protected void readUrls (final Cursor cursor, final TweetCursorReader reader, final List<String> urls) {
		final String avatarUrl = reader.readAvatar(cursor);
		if (avatarUrl != null) urls.add(avatarUrl);

		final List<Meta> metas = getDb().getTweetMetasOfType(reader.readUid(cursor), MetaType.MEDIA);
		if (metas != null) {
			for (final Meta m : metas) {
				if (m.getData() != null) urls.add(m.getData());
			}
		}
	}

	@Override
	protected void makeJobs (final List<String> urls, final Map<String, Callable<?>> jobs) {
		final HybridBitmapCache hybridBitmapCache = new HybridBitmapCache(this, 0);
		for (final String url : urls) {
			if (!hybridBitmapCache.touchFileIfExists(url)) {
				jobs.put(url, new FetchPicture(hybridBitmapCache, url));
			}
		}
	}

}
