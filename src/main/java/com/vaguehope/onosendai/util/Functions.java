package com.vaguehope.onosendai.util;

import com.vaguehope.onosendai.util.CollectionHelper.Function;

public final class Functions {

	private Functions () {
		throw new AssertionError();
	}

	public static final Function<Titleable, String> TITLE = new Function<Titleable, String>() {
		@Override
		public String exec (final Titleable input) {
			return input != null ? input.getUiTitle() : null;
		}
	};

}
