package com.vaguehope.onosendai.update;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.ColumnFeed;


@RunWith(MockitoJUnitRunner.class)
public class FetchFeedRequestTest {

	@Mock private Column column;
	@Mock private ColumnFeed columnFeed;
	@Mock private Account account;

	@Test
	public void itDoesEquals () throws Exception {
		final FetchFeedRequest a = new FetchFeedRequest(this.column, this.columnFeed, this.account);
		final FetchFeedRequest b = new FetchFeedRequest(this.column, this.columnFeed, this.account);
		assertEquals(a, b);
	}

}
