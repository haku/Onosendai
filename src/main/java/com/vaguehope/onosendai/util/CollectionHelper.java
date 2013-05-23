package com.vaguehope.onosendai.util;

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

}
