package com.vaguehope.onosendai.util;

/**
 * A very simplistic presentation of a Map.
 */
public interface MutableState<K, V> {

	V put (K key, V value);

	V get (K key);

}
