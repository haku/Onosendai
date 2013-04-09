package com.vaguehope.onosendai.model;

import static org.junit.Assert.assertSame;

import org.junit.Test;

public class MetaTypeTest {

	@Test
	public void itParsesCorrectly () throws Exception {
		for (MetaType t : MetaType.values()) {
			assertSame(t, MetaType.parseId(t.getId()));
		}
	}

}
