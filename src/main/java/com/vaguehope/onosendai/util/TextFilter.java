package com.vaguehope.onosendai.util;

import java.util.Locale;

public enum TextFilter {
	UPSIDE_DOWN("Upside Down") {
		@Override
		public String apply (final String in) {
			return upsideDown(in);
		}
	},
	HEAVY_METAL_UMLAUTS("Heavy Metal Umlauts") {
		@Override
		public String apply (final String in) {
			return heavyMetalUmlauts(in);
		}
	};

	private final String title;

	private TextFilter (final String title) {
		this.title = title;
	}

	@Override
	public String toString () {
		return this.title;
	}

	public abstract String apply (String in);

	private static final char[] UMLAUTABLE = { 'A', 'O', 'U' };
	private static final char[] UMLAUTED = { 'Ä', 'Ö', 'Ü' };

	protected static String heavyMetalUmlauts (final String in) {
		final String upperCase = in.toUpperCase(Locale.UK);
		final char[] out = upperCase.toCharArray();
		int quota = occurrences(out, UMLAUTABLE);
		if (quota < 1) return upperCase;
		quota = Math.max(1, quota / 3);
		while (quota > 0) {
			int x = (int) (Math.random() * out.length);
			x = nextAfter(out, x, UMLAUTABLE);
			if (x < 0) x = nextAfter(out, 0, UMLAUTABLE);
			out[x] = UMLAUTED[indexOf(out[x], UMLAUTABLE)];
			quota -= 1;
		}
		return new String(out);
	}

	private static final String INVERTABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private static final String INVERTED = "∀qƆpƎℲפHIſʞ˥WNOԀQɹS┴∩ΛMX⅄Zɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz";

	protected static String upsideDown (final String in) {
		final char[] arr = in.toCharArray();
		for (int i = 0; i < arr.length; i++) {
			final int x = INVERTABLE.indexOf(arr[i]);
			if (x >= 0) arr[i] = INVERTED.charAt(x);
		}
		reverse(arr);
		return new String(arr);
	}

	private static int occurrences (final char[] s, final char... chars) {
		int n = 0;
		for (final char c : s) {
			for (final char d : chars) {
				if (c == d) n += 1;
			}
		}
		return n;
	}

	private static int nextAfter (final char[] s, final int x, final char... chars) {
		for (int i = x; i < s.length; i++) {
			for (final char c : chars) {
				if (s[i] == c) return i;
			}
		}
		return -1;
	}

	private static int indexOf (final char c, final char[] arr) {
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == c) return i;
		}
		return -1;
	}

	private static void reverse (final char[] arr) {
		for (int i = 0; i < arr.length / 2; i++) {
			final char temp = arr[i];
			arr[i] = arr[arr.length - i - 1];
			arr[arr.length - i - 1] = temp;
		}
	}

}
