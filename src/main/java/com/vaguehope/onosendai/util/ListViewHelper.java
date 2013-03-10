package com.vaguehope.onosendai.util;

import android.view.View;
import android.widget.ListView;

public final class ListViewHelper {

	private ListViewHelper () {
		throw new AssertionError();
	}

	public static class ScrollState {

		final int index;
		final int top;

		public ScrollState (final int index, final int top) {
			this.index = index;
			this.top = top;
		}

		@Override
		public String toString () {
			return new StringBuilder()
					.append("SaveScrollState{").append(this.index).append(',').append(this.top).append('}')
					.toString();
		}

	}

	public static ScrollState saveScrollState (final ListView lv) {
		int index = lv.getFirstVisiblePosition();
		View v = lv.getChildAt(0);
		int top = (v == null) ? 0 : v.getTop();
		return new ScrollState(index, top);
	}

	public static void restoreScrollState (final ListView lv, final ScrollState state) {
		if (state == null) return;
		lv.setSelectionFromTop(state.index, state.top);
	}

}
