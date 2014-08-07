package com.vaguehope.onosendai.util;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import org.junit.Test;

public class IoHelperTest {

	@Test
	public void itToStringsInputStream () throws Exception {
		assertEquals("12345\n67890", IoHelper.toString(new ByteArrayInputStream("12345\n67890".getBytes())));
	}

	@Test
	public void itToStringsInputStreamWithMaxLength () throws Exception {
		assertEquals("12345", IoHelper.toString(new ByteArrayInputStream("1234567890".getBytes()), 5));
		assertEquals("1234567890", IoHelper.toString(new ByteArrayInputStream("1234567890".getBytes()), 10));
		assertEquals("1234567890", IoHelper.toString(new ByteArrayInputStream("1234567890".getBytes()), 15));
	}

}
