package com.vaguehope.onosendai.model;

import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

public class ScrollState {

	private static final String KEY_ITEM_ID = "list_view_item_id";
	private static final String KEY_TOP = "list_view_top";

	private final long itemId;
	private final int top;

	public ScrollState (final long itemId, final int top) {
		this.itemId = itemId;
		this.top = top;
	}

	@Override
	public String toString () {
		return new StringBuilder()
				.append("SaveScrollState{").append(this.getItemId())
				.append(',').append(this.getTop())
				.append('}')
				.toString();
	}

	public long getItemId () {
		return this.itemId;
	}

	public int getTop () {
		return this.top;
	}

	public void applyTo (final ListView lv) {
		// NOTE if this seems unreliable try wrapping setSelection*() calls in lv.post(...).
		final ListAdapter adapter = lv.getAdapter();
		for (int i = 0; i < adapter.getCount(); i++) {
			if (adapter.getItemId(i) == getItemId()) {
				lv.setSelectionFromTop(i, getTop());
				return;
			}
		}
		lv.setSelection(lv.getCount() - 1);
	}


	public void addToBundle (final Bundle bundle) {
		if (bundle == null) return;
		bundle.putLong(KEY_ITEM_ID, getItemId());
		bundle.putInt(KEY_TOP, getTop());
	}

	public static ScrollState from (final ListView lv) {
		final int index = lv.getFirstVisiblePosition();
		final View v = lv.getChildAt(0);
		final int top = (v == null) ? 0 : v.getTop();

		final long itemId = lv.getAdapter().getItemId(index);
		if (itemId < 0) return null;
		return new ScrollState(itemId, top);
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

}