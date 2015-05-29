package com.vaguehope.onosendai.update;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.ColumnFeed;
import com.vaguehope.onosendai.model.Filters;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.provider.twitter.TwitterFeed;
import com.vaguehope.onosendai.provider.twitter.TwitterProvider;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.LogWrapper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ FetchColumn.class })
public class FetchColumnTest {

	private static final String ACC_ID = "a_32";
	private static final Integer COL_ID = 123;

	@Mock private LogWrapper logWrapper;
	@Mock private DbInterface db;
	@Mock private Account account;
	@Mock private Column column;
	@Mock private ColumnFeed feed;
	@Mock private ProviderMgr providerMgr;
	@Mock private Filters filters;

	@Mock private TwitterProvider twitterProvider;
	@Mock private TweetList tweetList;

	private FetchColumn undertest;

	@Before
	public void before () throws Exception {
		this.undertest = new FetchColumn(this.db, new FetchFeedRequest(this.column, this.feed, this.account), this.providerMgr, this.filters);
		Whitebox.setInternalState(FetchColumn.class, "LOG", this.logWrapper);

		when(this.account.getId()).thenReturn(ACC_ID);
		when(this.account.getProvider()).thenReturn(AccountProvider.TWITTER);
		when(this.column.getId()).thenReturn(COL_ID);
		when(this.column.getFeeds()).thenReturn(Collections.singleton(new ColumnFeed(ACC_ID, "TIMELINE")));

		when(this.providerMgr.getTwitterProvider()).thenReturn(this.twitterProvider);
		when(this.twitterProvider.getTweets(isA(TwitterFeed.class), eq(this.account), anyLong(), anyBoolean())).thenReturn(this.tweetList);
	}

	@Test
	public void itFindsCorrectSinceIdWhenFetchingFromTwitter () throws Exception {
		final long sinceId = 120394230492830123L;
		final Tweet sinceTweet = mock(Tweet.class);
		when(sinceTweet.getSid()).thenReturn(String.valueOf(sinceId));
		when(this.db.findTweetsWithMeta(COL_ID, MetaType.ACCOUNT, ACC_ID, 1)).thenReturn(Collections.singletonList(sinceTweet));

		this.undertest.call();

		verify(this.twitterProvider).getTweets(isA(TwitterFeed.class), eq(this.account), eq(sinceId), anyBoolean());
	}

}
