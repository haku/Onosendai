package com.vaguehope.onosendai.provider.twitter;

import static org.junit.Assert.assertArrayEquals;
import java.util.Arrays;

import org.junit.Test;

import com.vaguehope.onosendai.util.EqualHelper;

import twitter4j.ExtendedMediaEntity.Variant;

public class VariantOrderTest {

	@Test
	public void itDoesSomething () throws Exception {
		final Variant[] exp = new Variant[] {
				new Vrnt(200, "a"),
				new Vrnt(100, "a"),
				new Vrnt(0, "b"),
				new Vrnt(0, "c"),
				new Vrnt(0, null),
				null
		};
		final Variant[] in = new Variant[] {
				new Vrnt(0, "c"),
				new Vrnt(100, "a"),
				null,
				new Vrnt(0, null),
				new Vrnt(0, "b"),
				new Vrnt(200, "a"),
		};
		Arrays.sort(in, VariantOrder.INSTANCE);
		assertArrayEquals(exp, in);
	}

	private static class Vrnt implements Variant {

		private final int bitrate;
		private final String type;

		public Vrnt (final int bitrate, final String type) {
			this.bitrate = bitrate;
			this.type = type;
		}

		@Override
		public int getBitrate () {
			return this.bitrate;
		}

		@Override
		public String getContentType () {
			return this.type;
		}

		@Override
		public String getUrl () {
			throw new UnsupportedOperationException("Not implemented.");
		}

		@Override
		public String toString () {
			return String.format("{%s, %s}", this.bitrate, this.type);
		}

		@Override
		public boolean equals (final Object o) {
			if (o == this) return true;
			if (o == null) return false;
			final Vrnt that = (Vrnt) o;
			return this.bitrate == that.bitrate &&
					EqualHelper.equal(this.type, that.type);
		}

	}

}
