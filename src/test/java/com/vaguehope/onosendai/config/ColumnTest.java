package com.vaguehope.onosendai.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import com.vaguehope.onosendai.util.CollectionHelper;

public class ColumnTest {

	@Test
	public void itRoundTrips () throws Exception {
		Column c = new Column(12, "title", new ColumnFeed("accountid", "resource"), 15, CollectionHelper.setOf(1, 2), NotificationStyle.DEFAULT, InlineMediaStyle.NONE, false);
		String j = c.toJson().toString(2);
		Column c1 = Column.parseJson(j);
		assertEquals(c, c1);
	}

	@Test
	public void itClonesWithNewId () throws Exception {
		Column c = new Column(12, "title", new ColumnFeed("accountid", "resource"), 15, CollectionHelper.setOf(1, 2), NotificationStyle.DEFAULT, InlineMediaStyle.NONE, false);
		Column c1 = new Column(89, c);
		Column c2 = new Column(12, c1);
		assertEquals(c, c2);
	}

	@Test
	public void itClonesWithNewAccountId () throws Exception {
		final Account a1 = mock(Account.class);
		final Account a2 = mock(Account.class);
		when(a1.getId()).thenReturn("accountid");
		when(a2.getId()).thenReturn("newaccountid");

		Column c = new Column(12, "title", new ColumnFeed(a1.getId(), "resource"), 15, CollectionHelper.setOf(1, 2), NotificationStyle.DEFAULT, InlineMediaStyle.NONE, false);
		Column c1 = c.replaceAccount(a2);
		Column c2 = c1.replaceAccount(a1);
		assertEquals(c, c2);
	}

	@Test
	public void itClonesWithNewExcludeColumnIds () throws Exception {
		Column c = new Column(12, "title", new ColumnFeed("accountid", "resource"), 15, CollectionHelper.setOf(1, 2), NotificationStyle.DEFAULT, InlineMediaStyle.NONE, false);
		Column c1 = new Column(CollectionHelper.setOf(2), c);
		Column c2 = new Column(CollectionHelper.setOf(1, 2), c1);
		assertEquals(c, c2);
	}

	@Test
	public void itEqualsChecksExcludes () throws Exception {
		Column c = new Column(12, "title", new ColumnFeed("accountid", "resource"), 15, CollectionHelper.setOf(1, 2), NotificationStyle.DEFAULT, InlineMediaStyle.NONE, false);
		assertFalse(c.equals(new Column(12, "title", new ColumnFeed("accountid", "resource"), 15, CollectionHelper.setOf(1, 1), NotificationStyle.DEFAULT, InlineMediaStyle.NONE, false)));
	}

	@Test
	public void itEqualsChecksNotify () throws Exception {
		Column c = new Column(12, "title", new ColumnFeed("accountid", "resource"), 15, CollectionHelper.setOf(1, 2), NotificationStyle.DEFAULT, InlineMediaStyle.NONE, false);
		assertFalse(c.equals(new Column(12, "title", new ColumnFeed("accountid", "resource"), 15, CollectionHelper.setOf(1, 2), null, InlineMediaStyle.NONE, false)));
	}

	@Test
	public void itParsesComplexNotificationStyle () throws Exception {
		NotificationStyle ns = new NotificationStyle(true, false, true);
		Column c = new Column(12, "title", new ColumnFeed("accountid", "resource"), 15, CollectionHelper.setOf(1, 2), ns, InlineMediaStyle.NONE, false);
		String j = c.toJson().toString(2);
		Column c1 = Column.parseJson(j);
		assertEquals(c, c1);
	}

	@Test
	public void itDoesNotAllowNegativeIdsWhenParsingJson () throws Exception {
		try {
			Column.parseJson(new Column(-1, "title", new ColumnFeed("accountid", "resource"), 15, null, NotificationStyle.DEFAULT, InlineMediaStyle.NONE, false).toJson().toString(2));
		}
		catch (JSONException e) {
			assertThat(e.getMessage(), startsWith("Failed to parse column:"));
			assertThat(e.getMessage(), endsWith("Column ID must be positive a integer."));
		}
	}

	public void itDefaultsInlineMediaToFalse () throws Exception {
		JSONObject j = new Column(12, "title", new ColumnFeed("accountid", "resource"), 15, CollectionHelper.setOf(1, 2), NotificationStyle.DEFAULT, InlineMediaStyle.NONE, false).toJson();
		assertEquals(false, j.remove("inline_media"));
		Column c = Column.parseJson(j.toString(2));
		assertEquals(InlineMediaStyle.NONE, c.getInlineMediaStyle());
	}

	public void itRoundtripsInlineMediaEnabled () throws Exception {
		JSONObject j = new Column(12, "title", new ColumnFeed("accountid", "resource"), 15, CollectionHelper.setOf(1, 2), NotificationStyle.DEFAULT, InlineMediaStyle.INLINE, false).toJson();
		Column c = Column.parseJson(j.toString(2));
		assertEquals(InlineMediaStyle.INLINE, c.getInlineMediaStyle());
	}

	public void itRoundtripsInlineMediaSeamless () throws Exception {
		JSONObject j = new Column(12, "title", new ColumnFeed("accountid", "resource"), 15, CollectionHelper.setOf(1, 2), NotificationStyle.DEFAULT, InlineMediaStyle.SEAMLESS, false).toJson();
		Column c = Column.parseJson(j.toString(2));
		assertEquals(InlineMediaStyle.SEAMLESS, c.getInlineMediaStyle());
	}

	public void itMigratesInlineMediaFalseToNone () throws Exception {
		JSONObject j = new Column(12, "title", new ColumnFeed("accountid", "resource"), 15, CollectionHelper.setOf(1, 2), NotificationStyle.DEFAULT, InlineMediaStyle.SEAMLESS, false).toJson();
		j.put("inline_media", false);
		Column c = Column.parseJson(j.toString(2));
		assertEquals(InlineMediaStyle.NONE, c.getInlineMediaStyle());
	}

	public void itMigratesInlineMediaTrueToInline () throws Exception {
		JSONObject j = new Column(12, "title", new ColumnFeed("accountid", "resource"), 15, CollectionHelper.setOf(1, 2), NotificationStyle.DEFAULT, InlineMediaStyle.SEAMLESS, false).toJson();
		j.put("inline_media", true);
		Column c = Column.parseJson(j.toString(2));
		assertEquals(InlineMediaStyle.INLINE, c.getInlineMediaStyle());
	}

	@Test
	public void itRoundTripsMultipleFeeds () throws Exception {
		Column c = new Column(12, "title",
				CollectionHelper.setOf(
						new ColumnFeed("accountid1", "resource1"),
						new ColumnFeed("accountid2", "resource2"),
						new ColumnFeed("accountid3", "resource3")),
				15, CollectionHelper.setOf(1, 2), NotificationStyle.DEFAULT, InlineMediaStyle.NONE, false);
		String j = c.toJson().toString(2);
		Column c1 = Column.parseJson(j);
		assertEquals(c, c1);
	}

}
