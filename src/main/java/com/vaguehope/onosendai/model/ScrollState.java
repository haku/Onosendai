package com.vaguehope.onosendai.model;


public class ScrollState {

	private final long itemId;
	private final int top;

	public ScrollState (final long itemId, final int top) {
		this.itemId = itemId;
		this.top = top;
	}

	@Override
	public String toString () {
		return new StringBuilder()
				.append("SaveScrollState{").append(this.getItemId())
				.append(',').append(this.getTop())
				.append('}')
				.toString();
	}

	public long getItemId () {
		return this.itemId;
	}

	public int getTop () {
		return this.top;
	}

}