package com.vaguehope.onosendai.util;

public final class EqualHelper {

	private EqualHelper () {
		throw new AssertionError();
	}

	public static boolean equal(final Object aThis, final Object aThat){
		return aThis == null ? aThat == null : aThis.equals(aThat);
	}

}
