package com.vaguehope.onosendai.util;

import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.vaguehope.onosendai.model.ScrollState;

public final class ListViewHelper {

	static final String KEY_ITEM_ID = "list_view_item_id";
	static final String KEY_TOP = "list_view_top";

	private ListViewHelper () {
		throw new AssertionError();
	}

	public static ScrollState saveScrollState (final ListView lv) {
		int index = lv.getFirstVisiblePosition();
		View v = lv.getChildAt(0);
		int top = (v == null) ? 0 : v.getTop();

		long itemId = lv.getAdapter().getItemId(index);
		if (itemId < 0) return null;
		return new ScrollState(itemId, top);
	}

	public static void restoreScrollState (final ListView lv, final ScrollState state) {
		if (state == null) return;
		// NOTE if this seems unreliable try wrapping setSelection*() calls in lv.post(...);
		ListAdapter adapter = lv.getAdapter();
		for (int i = 0; i < adapter.getCount(); i++) {
			if (adapter.getItemId(i) == state.getItemId()) {
				lv.setSelectionFromTop(i, state.getTop());
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

	public static void toBundle (final ScrollState state, final Bundle bundle) {
		if (state == null || bundle == null) return;
		bundle.putLong(ListViewHelper.KEY_ITEM_ID, state.getItemId());
		bundle.putInt(ListViewHelper.KEY_TOP, state.getTop());
	}

}
