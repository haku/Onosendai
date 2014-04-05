package com.vaguehope.onosendai.provider.successwhale;

import com.vaguehope.onosendai.util.EqualHelper;
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

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof SuccessWhaleSource)) return false;
		final SuccessWhaleSource that = (SuccessWhaleSource) o;
		return EqualHelper.equal(this.fullurl, that.fullurl);
	}

	@Override
	public int hashCode () {
		return this.fullurl == null ? 0 : this.fullurl.hashCode();
	}

}
