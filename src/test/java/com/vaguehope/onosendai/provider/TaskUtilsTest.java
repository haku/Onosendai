package com.vaguehope.onosendai.provider;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import twitter4j.TwitterException;
import twitter4j.internal.http.HttpResponseCode;

import com.vaguehope.onosendai.model.TaskOutcome;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleException;

public class TaskUtilsTest {

	@Test
	public void itCountsGeneralExAsTemp () throws Exception {
		assertEquals(TaskOutcome.TEMPORARY_FAILURE, TaskUtils.failureType(new Exception("example.")));
	}

	@Test
	public void itCountsRuntimeExAsPerm () throws Exception {
		assertEquals(TaskOutcome.PERMANENT_FAILURE, TaskUtils.failureType(new RuntimeException("example.")));
	}

	@Test
	public void itCountsPermSwExAsPermanent () throws Exception {
		assertEquals(TaskOutcome.PERMANENT_FAILURE, TaskUtils.failureType(new SuccessWhaleException("example.", true)));
	}

	@Test
	public void itCountsNonPermSwExAsTemp () throws Exception {
		assertEquals(TaskOutcome.TEMPORARY_FAILURE, TaskUtils.failureType(new SuccessWhaleException("example.", false)));
	}

	@Test
	public void itCountsNetworkErrorAsTemp () throws Exception {
		assertEquals(TaskOutcome.TEMPORARY_FAILURE, TaskUtils.failureType(new TwitterException("", new IOException(), 0)));
	}

	@Test
	public void itCountsRateLimiteExceededAsTemp () throws Exception {
		assertEquals(TaskOutcome.TEMPORARY_FAILURE, TaskUtils.failureType(new TwitterException("", null, HttpResponseCode.TOO_MANY_REQUESTS)));
	}

	@Test
	public void itCounts300AsTemp() throws Exception {
		assertEquals(TaskOutcome.TEMPORARY_FAILURE, TaskUtils.failureType(new TwitterException("", null, 300)));
	}

	@Test
	public void itCounts400AsPerm() throws Exception {
		assertEquals(TaskOutcome.PERMANENT_FAILURE, TaskUtils.failureType(new TwitterException("", null, 400)));
	}

	@Test
	public void itCounts500AsTemp() throws Exception {
		assertEquals(TaskOutcome.TEMPORARY_FAILURE, TaskUtils.failureType(new TwitterException("", null, 500)));
	}

	/**
	 * errorCode=187 errorMessage="Status is a duplicate." httpCode=403
	 * https://dev.twitter.com/docs/error-codes-responses
	 */
	@Test
	public void itCounts187AsPerm () throws Exception {
		final TwitterException te = new TwitterException("", null, 403);
		Whitebox.setInternalState(te, "errorCode", 187);
		assertEquals(TaskOutcome.PREVIOUS_ATTEMPT_SUCCEEDED, TaskUtils.failureType(te));
	}

}
