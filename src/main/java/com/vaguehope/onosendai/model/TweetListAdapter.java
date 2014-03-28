package com.vaguehope.onosendai.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.vaguehope.onosendai.images.ImageLoader;

@Deprecated
public class TweetListAdapter extends BaseAdapter {

	private final boolean showInlineMedia;
	private final ImageLoader imageLoader;
	private final LayoutInflater layoutInflater;

	private TweetList listData;

	public TweetListAdapter (final Context context, final boolean showInlineMedia, final ImageLoader imageLoader) {
		this.showInlineMedia = showInlineMedia;
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
		return getTweet(position);
	}

	@Override
	public long getItemId (final int position) {
		final Tweet tweet = getTweet(position);
		if (tweet == null) return -1;
		return tweet.getUid();
	}

	@Override
	public int getViewTypeCount () {
		return TweetLayout.values().length;
	}

	@Override
	public int getItemViewType (final int position) {
		return tweetLayoutType(this.listData.getTweet(position)).getIndex();
	}

	public Tweet getTweet (final int position) {
		if (this.listData == null) return null;
		if (position >= this.listData.count()) return null;
		return this.listData.getTweet(position);
	}

	@Override
	public View getView (final int position, final View convertView, final ViewGroup parent) {
		final Tweet item = this.listData.getTweet(position);
		final TweetLayout layoutType = tweetLayoutType(item);

		View view = convertView;
		TweetRowView rowView;
		if (view == null) {
			view = this.layoutInflater.inflate(layoutType.getLayout(), null);
			rowView = layoutType.makeRowView(view);
			view.setTag(rowView);
		}
		else {
			rowView = (TweetRowView) view.getTag();
		}

		layoutType.applyTweetTo(item, rowView, this.imageLoader);

		return view;
	}

	private TweetLayout tweetLayoutType (final Tweet t) {
		return this.showInlineMedia && t.getInlineMediaUrl() != null ? TweetLayout.INLINE_MEDIA : TweetLayout.MAIN;
	}

}
