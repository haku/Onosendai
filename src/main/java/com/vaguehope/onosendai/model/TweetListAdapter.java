package com.vaguehope.onosendai.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.vaguehope.onosendai.R;

public class TweetListAdapter extends BaseAdapter {

	private final LayoutInflater layoutInflater;

	private TweetList listData;

	public TweetListAdapter (final Context context) {
		this.layoutInflater = LayoutInflater.from(context);
	}

	public void setInputData (final TweetList data) {
		this.listData = data;
		notifyDataSetChanged();
	}

	public TweetList getInputData () {
		return this.listData;
	}

	@Override
	public int getCount () {
		return this.listData == null ? 0 : this.listData.count();
	}

	@Override
	public Object getItem (final int position) {
		if (this.listData == null) return null;
		if (position >= this.listData.count()) return null;
		return this.listData.getTweet(position);
	}

	@Override
	public long getItemId (final int position) {
		if (this.listData == null) return -1;
		if (position >= this.listData.count()) return -1;
		return this.listData.getTweet(position).getId();
	}

	@Override
	public View getView (final int position, final View convertView, final ViewGroup parent) {
		View view = convertView;
		RowView rowView;
		if (view == null) {
			view = this.layoutInflater.inflate(R.layout.tweetlistrow, null);
			rowView = new RowView(
					(TextView) view.findViewById(R.id.txtTweet),
					(TextView) view.findViewById(R.id.txtName)
					);
			view.setTag(rowView);
		}
		else {
			rowView = (RowView) view.getTag();
		}

		Tweet item = this.listData.getTweet(position);
		rowView.tweet.setText(item.getBody());
		rowView.name.setText(item.getUsername());

		return view;
	}

	private static class RowView {

		public final TextView tweet;
		public final TextView name;

		public RowView (final TextView tweet, final TextView name) {
			this.tweet = tweet;
			this.name = name;
		}

	}

}
