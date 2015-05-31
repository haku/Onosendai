package com.vaguehope.onosendai.util;

import java.lang.reflect.Array;
import java.util.Collection;

public final class ArrayHelper {

	private ArrayHelper () {
		throw new AssertionError();
	}

	public static String join (final Collection<?> arr, final String sep) {
		final StringBuilder s = new StringBuilder();
		for (final Object obj : arr) {
			if (s.length() > 0) s.append(sep);
			s.append(obj.toString());
		}
		return s.toString();
	}

	public static String join (final Object[] arr, final String sep) {
		final StringBuilder s = new StringBuilder();
		for (final Object obj : arr) {
			if (s.length() > 0) s.append(sep);
			s.append(obj.toString());
		}
		return s.toString();
	}

	public static <T> T[] joinArrays (final Class<T> type, final T[]... arrs) {
		int total = 0;
		for (final T[] arr : arrs) {
			if (arr != null) total += arr.length;
		}
		final T[] ret = (T[]) Array.newInstance(type, total);
		int x = 0;
		for (final T[] arr : arrs) {
			if (arr != null) {
				System.arraycopy(arr, 0, ret, x, arr.length);
				x += arr.length;
			}
		}
		return ret;
	}

}
