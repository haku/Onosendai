package com.vaguehope.onosendai.provider.hosaka;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.ColumnFeed;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.util.CollectionHelper;

@RunWith(MockitoJUnitRunner.class)
public class HosakaColumnTest {

	@Mock private Column col;
	@Mock private Config conf;
	@Mock private Account account0;
	@Mock private Account account2;

	@Test
	public void itSortsBeforeHashing () throws Exception {
		final ColumnFeed cf0 = new ColumnFeed("a0", "r0");
		final ColumnFeed cf1 = new ColumnFeed("a0", "r1");
		final ColumnFeed cf2 = new ColumnFeed("a2", "r2");
		when(this.conf.getAccount("a0")).thenReturn(this.account0);
		when(this.conf.getAccount("a2")).thenReturn(this.account2);
		when(this.account0.getTitle()).thenReturn("A 0");
		when(this.account2.getTitle()).thenReturn("A 2");

		when(this.col.getFeeds()).thenReturn(CollectionHelper.setOf(cf0, cf1, cf2));
		final String h0 = HosakaColumn.columnHash(this.col, this.conf);

		when(this.col.getFeeds()).thenReturn(CollectionHelper.setOf(cf1, cf2, cf0));
		final String h1 = HosakaColumn.columnHash(this.col, this.conf);

		assertEquals(h0, h1);
	}

}
