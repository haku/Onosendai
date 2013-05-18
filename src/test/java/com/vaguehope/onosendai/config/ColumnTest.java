package com.vaguehope.onosendai.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ColumnTest {

	@Test
	public void itRoundTrips () throws Exception {
		Column c = new Column(12, "title", "accountid", "resource", 15, new int[] { 1, 2 }, true);
		String j = c.toJson().toString(2);
		Column c1 = Column.parseJson(j);
		assertEquals(c, c1);
	}

	@Test
	public void itClonesWithNewId () throws Exception {
		Column c = new Column(12, "title", "accountid", "resource", 15, new int[] { 1, 2 }, true);
		Column c1 = new Column(89, c);
		Column c2 = new Column(12, c1);
		assertEquals(c, c2);
	}

}
