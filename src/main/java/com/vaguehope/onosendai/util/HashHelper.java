package com.vaguehope.onosendai.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashHelper {

	private HashHelper () {
		throw new AssertionError();
	}

	public static BigInteger md5String (final String s) {
		final MessageDigest md = MD_MD5_FACTORY.get();
		md.update(s.getBytes(), 0, s.length());
		return new BigInteger(1, md.digest());
	}

	/**
	 * MessageDigest.getInstance("MD5") can take up to a second, so using this
	 * to cache it and improve performance.
	 */
	private static final ThreadLocal<MessageDigest> MD_MD5_FACTORY = new ThreadLocal<MessageDigest>() {
		@Override
		protected MessageDigest initialValue () {
			try {
				final MessageDigest md = MessageDigest.getInstance("MD5");
				md.reset();
				return md;
			}
			catch (final NoSuchAlgorithmException e) {
				throw new IllegalStateException("JVM is missing MD5.", e);
			}
		}
	};

}
