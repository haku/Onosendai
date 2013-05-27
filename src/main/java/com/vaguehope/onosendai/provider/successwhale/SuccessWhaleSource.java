package com.vaguehope.onosendai.provider.successwhale;

import com.vaguehope.onosendai.util.Titleable;

public class SuccessWhaleSource implements Titleable {

	private final String fullname;
	private final String fullurl;

	public SuccessWhaleSource (final String fullname, final String fullurl) {
		this.fullname = fullname;
		this.fullurl = fullurl;
	}

	public String getFullurl () {
		return this.fullurl;
	}

	@Override
	public String getUiTitle () {
		return this.fullname;
	}

}
