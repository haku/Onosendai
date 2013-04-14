package com.vaguehope.onosendai;

import java.util.Random;

public final class Ui {

	private Ui () {
		throw new AssertionError();
	}

	private static final Random RAND = new Random(System.currentTimeMillis());

	public static int notificationIcon () {
		switch (RAND.nextInt(2)) {
			case 0:
				return R.drawable.ic_ho_meji;
			case 1:
			default:
				return R.drawable.ic_saka_meji;
		}
	}

}
