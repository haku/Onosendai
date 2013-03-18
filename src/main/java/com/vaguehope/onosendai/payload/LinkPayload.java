package com.vaguehope.onosendai.payload;

public class LinkPayload extends Payload {

	private final String url;

	public LinkPayload (final String url) {
		super(PayloadType.LINK);
		this.url = url;
	}

	public String getUrl () {
		return this.url;
	}

	@Override
	public String getTitle () {
		return this.url;
	}

}
