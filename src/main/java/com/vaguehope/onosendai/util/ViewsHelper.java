package com.vaguehope.onosendai.util;

import java.util.ArrayList;
import java.util.List;

import android.view.View;
import android.view.ViewGroup;

public final class ViewsHelper {

	private ViewsHelper () {
		throw new AssertionError();
	}

	public static <T extends View> List<T> findViewsByType (final ViewGroup parent, final Class<T> type, final int maxDepth) {
		return findViewsByType(parent, type, maxDepth, 0);
	}

	@SuppressWarnings("unchecked") // I have no idea how to avoid this.
	private static <T extends View> List<T> findViewsByType (final ViewGroup parent, final Class<T> type, final int maxDepth, final int depth) {
		final List<T> views = new ArrayList<T>();
		final int childCount = parent.getChildCount();
		for (int i = 0; i < childCount; i++) {
			final View child = parent.getChildAt(i);
			if (child instanceof ViewGroup && depth < maxDepth) views.addAll(findViewsByType((ViewGroup) child, type, maxDepth, depth + 1));
			if (type.isAssignableFrom(child.getClass())) views.add((T) child);
		}
		return views;
	}

}
