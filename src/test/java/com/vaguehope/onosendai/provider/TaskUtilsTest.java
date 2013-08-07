package com.vaguehope.onosendai.provider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import twitter4j.TwitterException;
import twitter4j.internal.http.HttpResponseCode;

import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleException;

public class TaskUtilsTest {

	@Test
	public void itCountsGeneralExAsTemp () throws Exception {
		assertFalse(TaskUtils.isFailurePermanent(new Exception("example.")));
	}

	@Test
	public void itCountsRuntimeExAsPerm () throws Exception {
		assertTrue(TaskUtils.isFailurePermanent(new RuntimeException("example.")));
	}

	@Test
	public void itCountsPermSwExAsPermanent () throws Exception {
		assertTrue(TaskUtils.isFailurePermanent(new SuccessWhaleException("example.", true)));
	}

	@Test
	public void itCountsNonPermSwExAsTemp () throws Exception {
		assertFalse(TaskUtils.isFailurePermanent(new SuccessWhaleException("example.", false)));
	}

	@Test
	public void itCountsNetworkErrorAsTemp () throws Exception {
		assertFalse(TaskUtils.isFailurePermanent(new TwitterException("", new IOException(), 0)));
	}

	@Test
	public void itCountsRateLimiteExceededAsTemp () throws Exception {
		assertFalse(TaskUtils.isFailurePermanent(new TwitterException("", null, HttpResponseCode.TOO_MANY_REQUESTS)));
	}

	@Test
	public void itCounts300AsTemp() throws Exception {
		assertFalse(TaskUtils.isFailurePermanent(new TwitterException("", null, 300)));
	}

	@Test
	public void itCounts400AsPerm() throws Exception {
		assertTrue(TaskUtils.isFailurePermanent(new TwitterException("", null, 400)));
	}

	@Test
	public void itCounts500AsTemp() throws Exception {
		assertFalse(TaskUtils.isFailurePermanent(new TwitterException("", null, 500)));
	}

}
