package com.vaguehope.onosendai.util;

import java.util.Arrays;
import java.util.Collection;

public class CollectionHelper {

	private CollectionHelper () {
		throw new AssertionError();
	}

	public static <I, O, C extends Collection<O>> C map (final I[] input, final Function<I, O> funciton, final C output) {
		for (I i : input) {
			output.add(funciton.exec(i));
		}
		return output;
	}

	public static <I, O, C extends Collection<O>> C map (final Collection<I> input, final Function<I, O> funciton, final C output) {
		for (I i : input) {
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

}
