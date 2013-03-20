package com.vaguehope.onosendai.payload;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.vaguehope.onosendai.util.ImageFetcherTask;
import com.vaguehope.onosendai.util.ImageFetcherTask.ImageFetchRequest;

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
		final Payload item = this.listData.getPayload(position);

		View view = convertView;
		RowView rowView;
		if (view == null) {
			view = this.layoutInflater.inflate(item.getLayout(), null);
			rowView = item.makeRowView(view);
			view.setTag(rowView);
		}
		else {
			rowView = (RowView) view.getTag();
		}

		rowView.getMain().setText(item.getTitle());
		if (item.getType() == PayloadType.MEDIA) {
			MediaPayload media = (MediaPayload) item;
			new ImageFetcherTask().execute(new ImageFetchRequest(media.getUrl(), rowView.getImage()));
		}

		return view;
	}

	static class RowView {

		private final TextView main;
		private final ImageView image;

		public RowView (final TextView main) {
			this(main, null);
		}

		public RowView (final TextView main, final ImageView image) {
			this.main = main;
			this.image = image;
		}

		public TextView getMain () {
			return this.main;
		}

		public ImageView getImage () {
			return this.image;
		}

	}

}
