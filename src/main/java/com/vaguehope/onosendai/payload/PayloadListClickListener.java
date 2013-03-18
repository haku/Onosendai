package com.vaguehope.onosendai.payload;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class PayloadListClickListener implements OnItemClickListener {

	private final Context context;
	private final PayloadListAdapter payloadAdaptor;

	public PayloadListClickListener (final Context context, final PayloadListAdapter payloadAdaptor) {
		this.context = context;
		this.payloadAdaptor = payloadAdaptor;
	}

	@Override
	public void onItemClick (final AdapterView<?> parent, final View view, final int position, final long id) {
		Payload payload = this.payloadAdaptor.getInputData().getPayload(position);
		if (payload.intentable()) {
			this.context.startActivity(payload.toIntent());
		}
		else {
			Toast.makeText(this.context, "Do not know how to show: " + payload.getTitle(), Toast.LENGTH_LONG).show();
		}
	}


}
