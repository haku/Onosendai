package com.vaguehope.onosendai.model;

import java.util.Arrays;

import com.vaguehope.onosendai.util.EqualHelper;
import com.vaguehope.onosendai.util.StringHelper;

public class Meta {

	private final MetaType type;
	private final String title;
	private final String data;

	public Meta (final MetaType type, final String data) {
		this(type, data, null);
	}

	public Meta (final MetaType type, final String data, final String title) {
		this.type = type;
		this.data = data;
		this.title = title;
	}

	public MetaType getType () {
		return this.type;
	}

	public String getData () {
		return this.data;
	}

	public String getTitle () {
		return this.title;
	}

	public boolean hasData() {
		return !StringHelper.isEmpty(this.data);
	}

	public boolean hasTitle () {
		return !StringHelper.isEmpty(this.title);
	}

	public int toInt (final int defaultValue) {
		if (!hasData()) return defaultValue;
		return Integer.parseInt(this.data);
	}

	public long toLong (final long defaultValue) {
		if (!hasData()) return defaultValue;
		return Long.parseLong(this.data);
	}

	@Override
	public String toString () {
		return new StringBuilder("Meta{").append(this.type)
				.append(",").append(this.data)
				.append(",").append(this.title)
				.append("}").toString();
	}

	@Override
	public int hashCode () {
		return Arrays.hashCode(new Object[] { this.type, this.data, this.title });
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof Meta)) return false;
		final Meta that = (Meta) o;
		return EqualHelper.equal(this.type, that.type)
				&& EqualHelper.equal(this.data, that.data)
				&& EqualHelper.equal(this.title, that.title);
	}

}
