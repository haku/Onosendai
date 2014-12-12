package com.vaguehope.onosendai.model;

import java.util.LinkedHashMap;

import com.vaguehope.onosendai.util.MutableState;

public class TweetListViewState {

	final LruMap<String, Boolean> expandedImages = new LruMap<String, Boolean>(5, 30); // 30 is probably enough unless your screen is reallly looong, long enough to fit 30 expanded images.

	public TweetListViewState () {}

	public MutableState<String, Boolean> getExpandedImagesTracker () {
		return this.expandedImages;
	}

	private static class LruMap<K, V> extends LinkedHashMap<K, V> implements MutableState<K, V> {

		private static final long serialVersionUID = -610834208932716056L;

		private final int maxSize;

		public LruMap (final int initialSize, final int maxSize) {
			super(initialSize, 0.75f, true);
			this.maxSize = maxSize;
		}

		@Override
		protected boolean removeEldestEntry (final java.util.Map.Entry<K, V> eldest) {
			return size() >= this.maxSize;
		}

	}

}
