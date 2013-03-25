package com.vaguehope.onosendai.util;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class ArrayHelperTest {

	@Test
	public void itJoinsArrays () throws Exception {
		String[] expected = new String[]{"a", "b"};
		String[] actual = ArrayHelper.joinArrays(String.class, new String[] {"a"}, new String[] {"b"});
		assertArrayEquals(expected, actual);
	}

	@Test
	public void itIgnoresNills () throws Exception {
		String[] expected = new String[]{"a"};
		String[] actual = ArrayHelper.joinArrays(String.class, new String[] {"a"}, null);
		assertArrayEquals(expected, actual);
	}
}
