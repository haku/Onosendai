package com.vaguehope.onosendai.util;

import java.lang.reflect.Array;

public final class ArrayHelper {

	private ArrayHelper () {
		throw new AssertionError();
	}

	public static <T> T[] joinArrays (final Class<T> type, final T[]... arrs) {
		int total = 0;
		for (T[] arr : arrs) {
			if (arr != null) total += arr.length;
		}
		T[] ret = (T[]) Array.newInstance(type, total);
		int x = 0;
		for (T[] arr : arrs) {
			if (arr != null) {
				System.arraycopy(arr, 0, ret, x, arr.length);
				x += arr.length;
			}
		}
		return ret;
	}

}
