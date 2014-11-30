package com.vaguehope.onosendai.model;

import android.widget.ImageView;
import android.widget.TextView;

import com.vaguehope.onosendai.images.ImageLoadRequest.ImageLoadListener;
import com.vaguehope.onosendai.widget.PendingImage;

class TweetRowView {

	private final ImageView avatar;
	private final TextView tweet;
	private final TextView name;
	private final PendingImage inlineMedia;

	public TweetRowView (final ImageView avatar, final TextView tweet, final TextView name) {
		this(avatar, tweet, name, null);
	}

	public TweetRowView (final PendingImage inImageView) {
		this(null, null, null, inImageView);
	}

	public TweetRowView (final ImageView avatar, final TextView tweet, final TextView name, final PendingImage inImageView) {
		this.avatar = avatar;
		this.tweet = tweet;
		this.name = name;
		this.inlineMedia = inImageView;
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

	public ImageView getInlineMedia () {
		return this.inlineMedia.getImage();
	}

	public ImageLoadListener getInlineMediaLoadListener () {
		return this.inlineMedia.getImageLoadListener();
	}

}
