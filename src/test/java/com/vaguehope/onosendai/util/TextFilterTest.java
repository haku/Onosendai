package com.vaguehope.onosendai.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;

import org.junit.Test;

public class TextFilterTest {

	@Test
	public void itAddsUmlauts () throws Exception {
		assertThat(TextFilter.HEAVY_METAL_UMLAUTS.apply(""), equalTo(""));
		assertThat(TextFilter.HEAVY_METAL_UMLAUTS.apply("This is."), equalTo("THIS IS."));
		assertThat(TextFilter.HEAVY_METAL_UMLAUTS.apply("This is a test."), equalTo("THIS IS Ä TEST."));
		assertThat(TextFilter.HEAVY_METAL_UMLAUTS.apply("This is aaa test."), containsString("Ä"));
		assertThat(TextFilter.HEAVY_METAL_UMLAUTS.apply("This is ooo test."), containsString("Ö"));
		assertThat(TextFilter.HEAVY_METAL_UMLAUTS.apply("This is uuu test."), containsString("Ü"));
	}

	@Test
	public void itUpsideDowns () throws Exception {
		assertThat(TextFilter.UPSIDE_DOWN.apply(""), equalTo(""));
		assertThat(TextFilter.UPSIDE_DOWN.apply("This is a test."), equalTo(".ʇsǝʇ ɐ sᴉ sᴉɥ┴"));
	}

}
