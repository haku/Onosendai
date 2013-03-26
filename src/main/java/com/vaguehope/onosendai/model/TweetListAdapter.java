package com.vaguehope.onosendai.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoader;

public class TweetListAdapter extends BaseAdapter {

	private final LayoutInflater layoutInflater;
	private final ImageLoader imageLoader;

	private TweetList listData;

	public TweetListAdapter (final Context context, final ImageLoader imageLoader) {
		this.imageLoader = imageLoader;
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
		return this.listData.getTweet(position).getUid();
	}

	@Override
	public View getView (final int position, final View convertView, final ViewGroup parent) {
		View view = convertView;
		RowView rowView;
		if (view == null) {
			view = this.layoutInflater.inflate(R.layout.tweetlistrow, null);
			rowView = new RowView(
					(ImageView) view.findViewById(R.id.imgMain),
					(TextView) view.findViewById(R.id.txtTweet),
					(TextView) view.findViewById(R.id.txtName)
					);
			view.setTag(rowView);
		}
		else {
			rowView = (RowView) view.getTag();
		}

		Tweet item = this.listData.getTweet(position);
		rowView.getTweet().setText(item.getBody());
		rowView.getName().setText(item.getUsername() != null ? item.getUsername() : item.getFullname());

		String avatarUrl = item.getAvatarUrl();
		if (avatarUrl != null) {
			this.imageLoader.loadImage(new ImageLoadRequest(avatarUrl, rowView.getAvatar()));
		}
		else {
			rowView.getAvatar().setImageResource(R.drawable.question_blue);
		}

		return view;
	}

	private static class RowView {

		private final ImageView avatar;
		private final TextView tweet;
		private final TextView name;

		public RowView (final ImageView avatar, final TextView tweet, final TextView name) {
			this.avatar = avatar;
			this.tweet = tweet;
			this.name = name;
		}

		public ImageView getAvatar () {
			return this.avatar;
		}

		public TextView getTweet () {
			return this.tweet;
		}

		public TextView getName () {
			return this.name;
		}

	}

}
