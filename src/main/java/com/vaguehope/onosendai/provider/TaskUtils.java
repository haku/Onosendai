package com.vaguehope.onosendai.provider;

import twitter4j.TwitterException;

import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleException;
import com.vaguehope.onosendai.util.StringHelper;

public final class TaskUtils {

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

	public static boolean isFailurePermanent(final Exception e) {
		if (e instanceof TwitterException) {
			final TwitterException te = (TwitterException) e;
			if (te.isCausedByNetworkIssue()) return false;
			if (te.exceededRateLimitation()) return false;
			final int code = te.getStatusCode();
			return code >= 400 && code < 500; // NOSONAR not magic numbers, this is HTTP spec.
		}
		else if (e instanceof SuccessWhaleException) {
			return ((SuccessWhaleException) e).isPermanent();
		}
		else if (e instanceof RuntimeException) {
			return true;
		}
		return false;
	}

}
