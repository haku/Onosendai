package com.vaguehope.onosendai.config;

import java.util.Locale;

import com.vaguehope.onosendai.util.Titleable;

public enum InlineMediaStyle implements Titleable {

	/**
	 * Synonym for null.
	 */
	NONE("none", "-"), //ES
	INLINE("inline", "Inline"), //ES
	SEAMLESS("seamless", "Seamless"); //ES

	private final String uiTitle;
	private final String serial;

	private InlineMediaStyle (final String serial, final String uiTitle) {
		this.serial = serial;
		this.uiTitle = uiTitle;
	}

	@Override
	public String getUiTitle () {
		return this.uiTitle;
	}

	@Override
	public String toString () {
		return getUiTitle();
	}

	public String serialise () {
		return this.serial;
	}

	public static InlineMediaStyle parseJson (final Object obj) {
		if (obj == null) return NONE;
		if (obj instanceof String) return parseJson((String) obj);
		if (obj instanceof Boolean) return ((Boolean) obj).booleanValue() ? INLINE : NONE;
		throw new IllegalArgumentException("Unexpected object type " + obj.getClass() + ": " + obj);
	}

	public static InlineMediaStyle parseJson (final String serial) {
		if (serial == null) return null;
		final String lowerSerial = serial.toLowerCase(Locale.ENGLISH);
		for (final InlineMediaStyle ims : values()) {
			if (lowerSerial.equals(ims.serial)) return ims;
		}
		throw new IllegalArgumentException("Unknown inline media style: '" + serial + "'.");
	}

}
