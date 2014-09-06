package com.vaguehope.onosendai.provider;

import com.vaguehope.onosendai.model.TaskOutcome;

class SendResult <T> {

	private final TaskOutcome outcome;
	private final T request;
	private final Exception e;

	public SendResult (final T request) {
		this.outcome = TaskOutcome.SUCCESS;
		this.request = request;
		this.e = null;
	}

	public SendResult (final T request, final Exception e) {
		this.outcome = TaskUtils.failureType(e);
		this.request = request;
		this.e = e;
	}

	public TaskOutcome getOutcome () {
		return this.outcome;
	}

	public T getRequest () {
		return this.request;
	}

	public Exception getE () {
		return this.e;
	}

	public String getEmsg () {
		return TaskUtils.getEmsg(this.e);
	}

}