package com.vaguehope.onosendai.provider.successwhale;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.ColumnFeed;

public class ColumnsXmlTest {

	private static final String ACCOUNT_ID = "ac0";
	private Account account;

	@Before
	public void before () throws Exception {
		this.account = new Account(ACCOUNT_ID, null, null, null, null, null, null);
	}

	@Test
	public void itParsesAllColumns () throws Exception {
		final ColumnsXml cx = new ColumnsXml(this.account, getClass().getResourceAsStream("/successwhale_columns.xml"));
		final SuccessWhaleColumns c = cx.getColumns();

		final List<Column> columns = c.getColumns();
		assertEquals(3, columns.size());

		assertColumn(columns.get(0), 0, "@twittername's Home Timeline", "twitter/12345678/statuses/home_timeline");
		assertColumn(columns.get(1), 1, "Somelist", "twitter/12345678/twittername/lists/somelist/statuses");
		assertColumn(columns.get(2), 2, "Combined Feed", "twitter/12345678/statuses/mentions:twitter/12345678/statuses/user_timeline");
	}

	private static void assertColumn (final Column c, final int id, final String title, final String resource) {
		assertEquals(id, c.getId());
		assertEquals(title, c.getTitle());
		assertEquals(Collections.singleton(new ColumnFeed(ACCOUNT_ID, resource)), c.getFeeds());
		assertEquals(null, c.getExcludeColumnIds());
		assertEquals(30, c.getRefreshIntervalMins());
	}

}
