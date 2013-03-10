package com.vaguehope.onosendai.util;

import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

public final class ListViewHelper {

	private static final String KEY_ITEM_ID = "list_view_item_id";
	private static final String KEY_TOP = "list_view_top";

	private ListViewHelper () {
		throw new AssertionError();
	}

	public static ScrollState saveScrollState (final ListView lv) {
		int index = lv.getFirstVisiblePosition();
		View v = lv.getChildAt(0);
		int top = (v == null) ? 0 : v.getTop();

		long itemId = lv.getAdapter().getItemId(index);

		return new ScrollState(itemId, top);
	}

	public static void restoreScrollState (final ListView lv, final ScrollState state) {
		if (state == null) return;
		// NOTE if this seems unreliable try wrapping setSelection*() calls in lv.post(...);
		ListAdapter adapter = lv.getAdapter();
		for (int i = 0; i < adapter.getCount(); i++) {
			if (adapter.getItemId(i) == state.itemId) {
				lv.setSelectionFromTop(i, state.top);
				return;
			}
		}
		lv.setSelection(lv.getCount() - 1);
	}

	public static ScrollState fromBundle (final Bundle bundle) {
		if (bundle == null) return null;
		if (bundle.containsKey(KEY_ITEM_ID) && bundle.containsKey(KEY_TOP)) {
			long itemId = bundle.getLong(KEY_ITEM_ID);
			int top = bundle.getInt(KEY_TOP);
			return new ScrollState(itemId, top);
		}
		return null;
	}

	public static class ScrollState {

		final long itemId;
		final int top;

		public ScrollState (final long itemId, final int top) {
			this.itemId = itemId;
			this.top = top;
		}

		public void writeTo (final Bundle bundle) {
			bundle.putLong(KEY_ITEM_ID, this.itemId);
			bundle.putInt(KEY_TOP, this.top);
		}

		@Override
		public String toString () {
			return new StringBuilder()
					.append("SaveScrollState{").append(this.itemId)
					.append(',').append(this.top)
					.append('}')
					.toString();
		}

	}

}
