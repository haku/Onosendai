package com.vaguehope.onosendai.payload;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoadRequest.ImageLoadListener;
import com.vaguehope.onosendai.images.ImageLoader;

public class PayloadListAdapter extends BaseAdapter {

	private final LayoutInflater layoutInflater;
	private final ImageLoader imageLoader;

	private PayloadList listData;

	public PayloadListAdapter (final Context context, final ImageLoader imageLoader) {
		this.layoutInflater = LayoutInflater.from(context);
		this.imageLoader = imageLoader;
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
	public int getViewTypeCount () {
		return PayloadLayout.values().length;
	}

	@Override
	public int getItemViewType (final int position) {
		return this.listData.getPayload(position).getLayout().getIndex();
	}

	@Override
	public View getView (final int position, final View convertView, final ViewGroup parent) {
		final Payload item = this.listData.getPayload(position);

		View view = convertView;
		RowView rowView;
		if (view == null) {
			view = this.layoutInflater.inflate(item.getLayout().getLayout(), null);
			rowView = item.makeRowView(view);
			view.setTag(rowView);
		}
		else {
			rowView = (RowView) view.getTag();
		}

		rowView.setText(item.getTitle());
		if (item.getType() == PayloadType.MEDIA) {
			MediaPayload media = (MediaPayload) item;
			this.imageLoader.loadImage(new ImageLoadRequest(media.getUrl(), rowView.getImage(), new CaptionRemover(rowView)));
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

		public void setText (final String text) {
			if (this.main == null) return;
			this.main.setText(text);
			this.main.setVisibility(View.VISIBLE);
		}

		public void hideText () {
			this.main.setVisibility(View.GONE);
		}

		public ImageView getImage () {
			return this.image;
		}

	}

	private static class CaptionRemover implements ImageLoadListener {

		private final RowView rowView;

		public CaptionRemover (final RowView rowView) {
			this.rowView = rowView;
		}

		@Override
		public void imageLoaded (final ImageLoadRequest req) {
			this.rowView.hideText();
		}

	}

}
