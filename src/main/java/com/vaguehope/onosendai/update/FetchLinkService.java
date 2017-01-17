package com.vaguehope.onosendai.update;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import android.content.Context;
import android.database.Cursor;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.MetaUtils;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.provider.twitter.TwitterUrls;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.storage.TweetCursorReader;
import com.vaguehope.onosendai.ui.pref.FetchingPrefFragment;
import com.vaguehope.onosendai.update.FetchLinkService.AccountColumnMeta;
import com.vaguehope.onosendai.util.LogWrapper;

public class FetchLinkService extends AbstractBgFetch<AccountColumnMeta> {

	protected static class AccountColumnMeta {
		private final Account account;
		private final Column column;
		private final Meta meta;

		public AccountColumnMeta (final Account account, final Column column, final Meta meta) {
			this.account = account;
			this.column = column;
			this.meta = meta;
		}

		public Account getAccount () {
			return this.account;
		}

		public Column getColumn () {
			return this.column;
		}


		public Meta getMeta () {
			return this.meta;
		}
	}

	private static final LogWrapper LOG = new LogWrapper("FLS");

	public static void startServiceIfConfigured (final Context context, final Prefs prefs, final Collection<Column> columns, final boolean manual) {
		AbstractBgFetch.startServiceIfConfigured(FetchLinkService.class, FetchingPrefFragment.KEY_PREFETCH_LINKS, context, prefs, columns, manual);
	}

	public FetchLinkService () {
		super(FetchLinkService.class, false, LOG);
	}

	@Override
	protected void readUrls (final Cursor cursor, final TweetCursorReader reader, final Column col, final Config conf, final List<AccountColumnMeta> retMetas) {
		final long uid = reader.readUid(cursor);
		final List<Meta> accountMetas = getDb().getTweetMetasOfType(uid, MetaType.ACCOUNT);
		final Account account = MetaUtils.accountFromMeta(accountMetas, conf);
		final List<Meta> urlMetas = getDb().getTweetMetasOfType(uid, MetaType.URL);
		addMetas(account, col, urlMetas, retMetas);
	}

	@Override
	protected void readUrls (final Tweet tweet, final Column col, final Config conf, final List<AccountColumnMeta> retMetas) {
		final Account account = MetaUtils.accountFromMeta(tweet, conf);
		addMetas(account, col, tweet.getMetas(), retMetas);
	}

	private static void addMetas (final Account account, final Column col, final List<Meta> tms, final List<AccountColumnMeta> retMetas) {
		if (tms != null) {
			for (final Meta m : tms) {
				if (m.getType() == MetaType.URL) {
					retMetas.add(new AccountColumnMeta(account, col, m));
				}
			}
		}
	}

	@Override
	protected void makeJobs (final List<AccountColumnMeta> acms, final ProviderMgr provMgr, final Map<String, Callable<?>> jobs) {
		final DbInterface db = getDb();
		for (final AccountColumnMeta acm : acms) {
			final Meta m = acm.getMeta();

			final String linkedTweetSid = TwitterUrls.readTweetSidFromUrl(m.getData());
			if (linkedTweetSid != null) {
				jobs.put(m.getData(), new FetchTweet(getDb(), provMgr, acm.getAccount(), acm.getColumn(), linkedTweetSid));
			}
			else if (FetchLinkTitle.shouldFetchTitle(m)) {
				if (!FetchLinkTitle.isTitleCached(db, m)) {
					jobs.put(m.getData(), new FetchLinkTitle(db, m));
				}
			}
		}
	}

}
