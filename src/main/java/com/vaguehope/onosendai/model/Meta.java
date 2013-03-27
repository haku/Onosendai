package com.vaguehope.onosendai.model;

import java.util.Arrays;

import com.vaguehope.onosendai.util.EqualHelper;

public class Meta {

	private final MetaType type;
	private final String data;

	public Meta (final MetaType type, final String data) {
		this.type = type;
		this.data = data;
	}

	public MetaType getType () {
		return this.type;
	}

	public String getData () {
		return this.data;
	}

	@Override
	public int hashCode () {
		return Arrays.hashCode(new Object[] { this.type, this.data });
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof Meta)) return false;
		Meta that = (Meta) o;
		return EqualHelper.equal(this.type, that.type)
				&& EqualHelper.equal(this.data, that.data);
	}

}
