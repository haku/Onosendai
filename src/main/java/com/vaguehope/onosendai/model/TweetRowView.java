package com.vaguehope.onosendai.model;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.vaguehope.onosendai.images.ImageLoadRequest.ImageLoadListener;
import com.vaguehope.onosendai.widget.ClickToExpand;
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

	public void showInlineMedia(final boolean show) {
		this.inlineMedia.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	public ImageView getInlineMedia () {
		return this.inlineMedia.getImage();
	}

	public ImageLoadListener getInlineMediaLoadListener () {
		return this.inlineMedia.getImageLoadListener();
	}

	public static class QuotingTweetRowView extends TweetRowView {

		private final ClickToExpand qcte;
		private final ImageView qAvatar;
		private final TextView qTweet;
		private final TextView qName;
		private final PendingImage qInlineMedia;

		public QuotingTweetRowView (
				final ImageView avatar, final TextView tweet, final TextView name, final PendingImage inImageView,
				final ClickToExpand qcte,
				final ImageView qAvatar, final TextView qTweet, final TextView qName, final PendingImage qInImageView) {
			super(avatar, tweet, name, inImageView);
			this.qcte = qcte;
			this.qAvatar = qAvatar;
			this.qTweet = qTweet;
			this.qName = qName;
			this.qInlineMedia = qInImageView;
		}

		public ClickToExpand getQcte () {
			return this.qcte;
		}

		public ImageView getQAvatar () {
			return this.qAvatar;
		}

		public TextView getQTweet () {
			return this.qTweet;
		}

		public TextView getQName () {
			return this.qName;
		}

		public void showQInlineMedia(final boolean show) {
			this.qInlineMedia.setVisibility(show ? View.VISIBLE : View.GONE);
		}

		public ImageView getQInlineMedia () {
			return this.qInlineMedia.getImage();
		}

		public ImageLoadListener getQInlineMediaLoadListener () {
			return this.qInlineMedia.getImageLoadListener();
		}

	}

}
