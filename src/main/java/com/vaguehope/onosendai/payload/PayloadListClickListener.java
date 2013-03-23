package com.vaguehope.onosendai.payload;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class PayloadListClickListener implements OnItemClickListener {

	public PayloadListClickListener () {}

	@Override
	public void onItemClick (final AdapterView<?> parent, final View view, final int position, final long id) {
		final ListView lv = (ListView) parent;
		final Payload payload = (Payload) lv.getAdapter().getItem(position);
		if (payload.intentable()) {
			parent.getContext().startActivity(payload.toIntent());
		}
		else {
			Toast.makeText(parent.getContext(), "Do not know how to show: " + payload.getTitle(), Toast.LENGTH_LONG).show();
		}
	}

}
