package com.vaguehope.onosendai.payload;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class PayloadListClickListener implements OnItemClickListener, OnItemLongClickListener {

	private final PayloadClickListener clickListener;

	public PayloadListClickListener (final PayloadClickListener clickListener) {
		this.clickListener = clickListener;
	}

	@Override
	public void onItemClick (final AdapterView<?> parent, final View view, final int position, final long id) {
		final ListView lv = (ListView) parent;
		final Payload payload = (Payload) lv.getAdapter().getItem(position);
		if (payload.intentable()) {
			parent.getContext().startActivity(payload.toIntent(parent.getContext()));
		}
		else if (!this.clickListener.payloadClicked(payload)) {
			Toast.makeText(parent.getContext(), "Do not know how to show: " + payload.getTitle(), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public boolean onItemLongClick (final AdapterView<?> parent, final View view, final int position, final long id) {
		final ListView lv = (ListView) parent;
		final Payload payload = (Payload) lv.getAdapter().getItem(position);
		if (payload.longClick(parent.getContext())) return true;
		return this.clickListener.payloadLongClicked(payload);
	}

}
