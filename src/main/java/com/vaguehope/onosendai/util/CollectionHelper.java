package com.vaguehope.onosendai.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CollectionHelper {

	private CollectionHelper () {
		throw new AssertionError();
	}

	public static <J, I extends J, O, C extends Collection<O>> C map (final I[] input, final Function<J, O> funciton, final C output) {
		for (final I i : input) {
			output.add(funciton.exec(i));
		}
		return output;
	}

	public static <J, I extends J, O, C extends Collection<O>> C map (final Collection<I> input, final Function<J, O> funciton, final C output) {
		for (final I i : input) {
			output.add(funciton.exec(i));
		}
		return output;
	}

	public interface Function<I, O> {
		O exec(I input);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static Collection<Integer> sequence(final int from, final int count) {
		final Integer[] arr = new Integer[count];
		for (int i = 0; i < count; i++) {
			arr[i] = Integer.valueOf(from + i);
		}
		return Arrays.asList(arr);
	}

	public static <T> List<T> listOf(final T... items) {
		return Collections.unmodifiableList(Arrays.asList(items));
	}

	/**
	 * Ordered as specified.
	 */
	public static <T> Set<T> setOf(final T... items) {
		return Collections.unmodifiableSet(new LinkedHashSet<T>(Arrays.asList(items)));
	}

	public static <O, C extends Collection<O>> C assertNoNulls(final C col) {
		for (final O i : col) {
			if (i == null) throw new IllegalArgumentException("Must not contain nulls: " + col);
		}
		return col;
	}

}
