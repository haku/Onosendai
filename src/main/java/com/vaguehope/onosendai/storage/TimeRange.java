package com.vaguehope.onosendai.storage;

public class TimeRange {
	public final long count;
	public final long rangeSeconds;

	public TimeRange (final long count, final long rangeSeconds) {
		this.count = count;
		this.rangeSeconds = rangeSeconds;
	}
}
