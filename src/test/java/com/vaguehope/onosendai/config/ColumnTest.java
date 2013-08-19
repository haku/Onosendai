package com.vaguehope.onosendai.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.json.JSONException;
import org.junit.Test;

import com.vaguehope.onosendai.util.CollectionHelper;

public class ColumnTest {

	@Test
	public void itRoundTrips () throws Exception {
		Column c = new Column(12, "title", "accountid", "resource", 15, CollectionHelper.setOf(1, 2), true);
		String j = c.toJson().toString(2);
		Column c1 = Column.parseJson(j);
		assertEquals(c, c1);
	}

	@Test
	public void itClonesWithNewId () throws Exception {
		Column c = new Column(12, "title", "accountid", "resource", 15, CollectionHelper.setOf(1, 2), true);
		Column c1 = new Column(89, c);
		Column c2 = new Column(12, c1);
		assertEquals(c, c2);
	}

	@Test
	public void itEqualsChecksExcludes () throws Exception {
		Column c = new Column(12, "title", "accountid", "resource", 15, CollectionHelper.setOf(1, 2), true);
		assertFalse(c.equals(new Column(12, "title", "accountid", "resource", 15, CollectionHelper.setOf(1, 1), true)));
	}

	@Test
	public void itEqualsChecksNotify () throws Exception {
		Column c = new Column(12, "title", "accountid", "resource", 15, CollectionHelper.setOf(1, 2), true);
		assertFalse(c.equals(new Column(12, "title", "accountid", "resource", 15, CollectionHelper.setOf(1, 2), false)));
	}

	@Test
	public void itDoesNotAllowNegativeIdsWhenParsingJson () throws Exception {
		try {
			Column.parseJson(new Column(-1, "title", "accountid", "resource", 15, null, true).toJson().toString(2));
		}
		catch (JSONException e) {
			assertEquals("Column ID must be positive a integer.", e.getMessage());
		}
	}

}
