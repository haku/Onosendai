package com.vaguehope.onosendai.model;


public class ScrollState {

	public final long itemId;
	public final int top;

	public ScrollState (final long itemId, final int top) {
		this.itemId = itemId;
		this.top = top;
	}

	@Override
	public String toString () {
		return new StringBuilder()
				.append("SaveScrollState{").append(this.itemId)
				.append(',').append(this.top)
				.append('}')
				.toString();
	}

}