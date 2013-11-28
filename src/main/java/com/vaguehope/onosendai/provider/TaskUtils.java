package com.vaguehope.onosendai.provider;

import twitter4j.TwitterException;

import com.vaguehope.onosendai.model.TaskOutcome;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleException;
import com.vaguehope.onosendai.util.StringHelper;

public final class TaskUtils {

	private static final int TWITTER_ERROR_CODE_STATUS_IS_A_DUPLICATE = 187;

	private TaskUtils () {
		throw new AssertionError();
	}

	public static String getEmsg(final Exception e) {
		if (e instanceof TwitterException) {
			final TwitterException te = (TwitterException) e;
			if (te.getErrorCode() >= 0 && te.getErrorMessage() != null) {
				return String.format("%s %s", te.getErrorCode(), te.getErrorMessage());
			}
		}
		return StringHelper.isEmpty(e.getMessage()) ? e.toString() : e.getMessage();
	}

	public static TaskOutcome failureType(final Exception e) {
		if (e instanceof TwitterException) {
			final TwitterException te = (TwitterException) e;
			if (te.isCausedByNetworkIssue()) return TaskOutcome.TEMPORARY_FAILURE;
			if (te.exceededRateLimitation()) return TaskOutcome.TEMPORARY_FAILURE;
			if (te.getErrorCode() == TWITTER_ERROR_CODE_STATUS_IS_A_DUPLICATE) return TaskOutcome.PREVIOUS_ATTEMPT_SUCCEEDED;
			final int code = te.getStatusCode();
			return code >= 400 && code < 500 ? TaskOutcome.PERMANENT_FAILURE : TaskOutcome.TEMPORARY_FAILURE; // NOSONAR not magic numbers, this is HTTP spec.
		}
		else if (e instanceof SuccessWhaleException) {
			return ((SuccessWhaleException) e).isPermanent() ? TaskOutcome.PERMANENT_FAILURE : TaskOutcome.TEMPORARY_FAILURE;
		}
		else if (e instanceof RuntimeException) {
			return TaskOutcome.PERMANENT_FAILURE;
		}
		return TaskOutcome.TEMPORARY_FAILURE;
	}

}
