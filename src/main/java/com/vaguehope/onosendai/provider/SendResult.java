package com.vaguehope.onosendai.provider;

import com.vaguehope.onosendai.model.TaskOutcome;
import com.vaguehope.onosendai.model.Tweet;

class SendResult <T> {

	private final TaskOutcome outcome;
	private final T request;
	private final Tweet response;
	private final Exception e;

	public SendResult (final T request) {
		this(request, (Tweet) null);
	}

	public SendResult (final T request, final Tweet response) {
		this.outcome = TaskOutcome.SUCCESS;
		this.request = request;
		this.response = response;
		this.e = null;
	}

	public SendResult (final T request, final Exception e) {
		this.outcome = TaskUtils.failureType(e);
		this.request = request;
		this.response = null;
		this.e = e;
	}

	public TaskOutcome getOutcome () {
		return this.outcome;
	}

	public T getRequest () {
		return this.request;
	}

	public Tweet getResponse () {
		return this.response;
	}

	public Exception getE () {
		return this.e;
	}

	public String getEmsg () {
		return TaskUtils.getEmsg(this.e);
	}

}