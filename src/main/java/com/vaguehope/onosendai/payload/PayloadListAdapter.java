package com.vaguehope.onosendai.payload;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.vaguehope.onosendai.R;

public class PayloadListAdapter extends BaseAdapter {

	private final LayoutInflater layoutInflater;

	private PayloadList listData;

	public PayloadListAdapter (final Context context) {
		this.layoutInflater = LayoutInflater.from(context);
	}

	public void setInputData (final PayloadList data) {
		this.listData = data;
		notifyDataSetChanged();
	}

	public PayloadList getInputData () {
		return this.listData;
	}

	@Override
	public int getCount () {
		return this.listData == null ? 0 : this.listData.size();
	}

	@Override
	public Object getItem (final int position) {
		if (this.listData == null) return null;
		if (position >= this.listData.size()) return null;
		return this.listData.getPayload(position);
	}

	@Override
	public long getItemId (final int position) {
		return position;
	}

	@Override
	public View getView (final int position, final View convertView, final ViewGroup parent) {
		View view = convertView;
		RowView rowView;
		if (view == null) {
			view = this.layoutInflater.inflate(R.layout.payloadlistrow, null);
			rowView = new RowView(
					(TextView) view.findViewById(R.id.txtMain)
					);
			view.setTag(rowView);
		}
		else {
			rowView = (RowView) view.getTag();
		}

		Payload item = this.listData.getPayload(position);
		rowView.main.setText(item.getTitle());

		return view;
	}

	private static class RowView {

		public final TextView main;

		public RowView (final TextView main) {
			this.main = main;
		}

	}

}
