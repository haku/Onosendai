package com.vaguehope.onosendai.payload;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.Tweet;

public class PayloadListAdapter extends BaseAdapter {

	private final LayoutInflater layoutInflater;
	private final ImageLoader imageLoader;
	private final View listView;
	private final PayloadClickListener clickListener;

	private Tweet tweet;
	private PayloadList listData;

	public PayloadListAdapter (final Context context, final ImageLoader imageLoader, final View listView, final PayloadClickListener clickListener) {
		this.listView = listView;
		this.clickListener = clickListener;
		this.layoutInflater = LayoutInflater.from(context);
		this.imageLoader = imageLoader;
	}

	public boolean isForTweet(final Tweet t) {
		return this.tweet.getUid() == t.getUid();
	}

	public void setInput (final Config config, final Tweet tweet) {
		this.tweet = tweet;
		this.listData = PayloadUtils.makePayloads(config, tweet);
		notifyDataSetChanged();
	}

	Payload findForMeta(final Meta meta) {
		return this.listData.findForMeta(meta);
	}

	void addItem (final Payload payload) {
		this.listData.addItem(payload);
		notifyDataSetChanged();
	}

	// FIXME refactor so this can be package private.
	public void addItemsTop (final List<Payload> payloads) {
		this.listData.addItemsTop(payloads);
		notifyDataSetChanged();
	}

	void addItemAfter(final Payload toAdd, final Payload marker) {
		this.listData.addItemAfter(toAdd, marker);
		notifyDataSetChanged();
	}

	void replaceItem(final Payload find, final Payload with) {
		this.listData.replaceItem(find, with);
		notifyDataSetChanged();
	}

	void replaceItem(final Payload find, final List<? extends Payload> withs) {
		this.listData.replaceItem(find, withs);
		notifyDataSetChanged();
	}

	void removeItem(final Payload payload) {
		this.listData.removeItem(payload);
		notifyDataSetChanged();
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
		PayloadRowView rowView;
		if (view == null) {
			view = this.layoutInflater.inflate(item.getLayout().getLayout(), null);
			rowView = item.makeRowView(view);
			view.setTag(rowView);
		}
		else {
			rowView = (PayloadRowView) view.getTag();
		}
		item.applyTo(rowView, this.imageLoader, this.listView.getWidth(), this.clickListener);
		return view;
	}

}
