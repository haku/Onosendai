package com.vaguehope.onosendai.payload;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;
import android.net.Uri;

import com.xtremelabs.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class HashTagPayloadTest {

	@Test
	public void itCreateUrlCorrectly () throws Exception {
		HashTagPayload tag = new HashTagPayload(null, "#123abc");
		Intent intent = tag.toIntent(null);
		assertEquals(Uri.parse("https://twitter.com/search?q=%23123abc"), intent.getData());
	}

}
