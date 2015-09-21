package com.vaguehope.onosendai.model;

import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.model.LinkedTweetLoader.TweetLoadRequest;
import com.vaguehope.onosendai.model.TweetRowView.QuotingTweetRowView;
import com.vaguehope.onosendai.storage.TweetCursorReader;
import com.vaguehope.onosendai.widget.PendingImage;

public enum TweetLayout {

	MAIN(0, R.layout.tweetlistrow) {
		@Override
		public TweetRowView makeRowView (final View view, final TweetListViewState tweetListViewState) {
			return new TweetRowView(
					(ImageView) view.findViewById(R.id.imgMain),
					(TextView) view.findViewById(R.id.txtTweet),
					(TextView) view.findViewById(R.id.txtName)
			);
		}

		@Override
		public void applyTweetTo (final Tweet item, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth, final LinkedTweetLoader tweetLoader) {
			if (item.isFiltered()) {
				rowView.getTweet().setText(R.string.tweet_filtered);
			}
			else {
				rowView.getTweet().setText(item.getBody());
			}

			final String usernameWithSubtitle = item.getUsernameWithSubtitle();
			rowView.getName().setText(usernameWithSubtitle != null ? usernameWithSubtitle : item.getFullnameWithSubtitle());

			final String avatarUrl = item.getAvatarUrl();
			if (avatarUrl != null) {
				imageLoader.loadImage(new ImageLoadRequest(avatarUrl, rowView.getAvatar()));
			}
			else {
				rowView.getAvatar().setImageResource(R.drawable.question_blue);
			}
		}

		@Override
		public void applyCursorTo (final Cursor c, final TweetCursorReader cursorReader, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth, final LinkedTweetLoader tweetLoader) {
			final String name;
			final String username = cursorReader.readUsernameWithSubtitle(c);
			if (username != null) {
				name = username;
			}
			else {
				name = cursorReader.readFullnameWithSubtitle(c);
			}
			rowView.getName().setText(name);

			if (cursorReader.readFiltered(c)) {
				rowView.getTweet().setText(R.string.tweet_filtered);
			}
			else {
				rowView.getTweet().setText(cursorReader.readBody(c));
			}

			final String avatarUrl = cursorReader.readAvatar(c);
			if (avatarUrl != null) {
				imageLoader.loadImage(new ImageLoadRequest(avatarUrl, rowView.getAvatar()));
			}
			else {
				rowView.getAvatar().setImageResource(R.drawable.question_blue);
			}
		}
	},
	INLINE_MEDIA(1, R.layout.tweetlistinlinemediarow) {
		@Override
		public TweetRowView makeRowView (final View view, final TweetListViewState tweetListViewState) {
			final PendingImage pendingImage = (PendingImage) view.findViewById(R.id.imgMedia);
			pendingImage.setExpandedTracker(tweetListViewState.getExpandedImagesTracker());
			return new TweetRowView(
					(ImageView) view.findViewById(R.id.imgMain),
					(TextView) view.findViewById(R.id.txtTweet),
					(TextView) view.findViewById(R.id.txtName),
					pendingImage
			);
		}

		@Override
		public void applyTweetTo (final Tweet item, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth, final LinkedTweetLoader tweetLoader) {
			MAIN.applyTweetTo(item, rowView, imageLoader, reqWidth, tweetLoader);
			setImage(item.getInlineMediaUrl(), rowView, imageLoader, reqWidth);
		}

		@Override
		public void applyCursorTo (final Cursor c, final TweetCursorReader cursorReader, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth, final LinkedTweetLoader tweetLoader) {
			MAIN.applyCursorTo(c, cursorReader, rowView, imageLoader, reqWidth, tweetLoader);
			setImage(cursorReader.readInlineMedia(c), rowView, imageLoader, reqWidth);
		}
	},
	QUOTED(2, R.layout.tweetlistquoterow) {
		@Override
		public TweetRowView makeRowView (final View view, final TweetListViewState tweetListViewState) {
			final View t = view.findViewById(R.id.tweet);
			final View qt = view.findViewById(R.id.quotedTweet);

			final PendingImage pendingImage = (PendingImage) t.findViewById(R.id.imgMedia);
			pendingImage.setExpandedTracker(tweetListViewState.getExpandedImagesTracker());

			final PendingImage qPendingImage = (PendingImage) qt.findViewById(R.id.imgMedia);
			qPendingImage.setExpandedTracker(tweetListViewState.getExpandedImagesTracker());

			return new QuotingTweetRowView(
					(ImageView) t.findViewById(R.id.imgMain),
					(TextView) t.findViewById(R.id.txtTweet),
					(TextView) t.findViewById(R.id.txtName),
					pendingImage,
					(ImageView) qt.findViewById(R.id.imgMain),
					(TextView) qt.findViewById(R.id.txtTweet),
					(TextView) qt.findViewById(R.id.txtName),
					qPendingImage
			);
		}

		@Override
		public void applyTweetTo (final Tweet item, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth, final LinkedTweetLoader tweetLoader) {
			MAIN.applyTweetTo(item, rowView, imageLoader, reqWidth, tweetLoader);
			final String inlineMediaUrl = item.getInlineMediaUrl();
			if (inlineMediaUrl != null) {
				setImage(inlineMediaUrl, rowView, imageLoader, reqWidth);
			}
			else {
				rowView.showInlineMedia(false);
			}
			tweetLoader.loadTweet(new TweetLoadRequest(item.getSid(), item.getQuotedSid(), (QuotingTweetRowView) rowView, imageLoader, reqWidth));
		}

		@Override
		public void applyCursorTo (final Cursor c, final TweetCursorReader cursorReader, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth, final LinkedTweetLoader tweetLoader) {
			MAIN.applyCursorTo(c, cursorReader, rowView, imageLoader, reqWidth, tweetLoader);
			final String inlineMediaUrl = cursorReader.readInlineMedia(c);
			if (inlineMediaUrl != null) {
				setImage(inlineMediaUrl, rowView, imageLoader, reqWidth);
			}
			else {
				rowView.showInlineMedia(false);
			}
			tweetLoader.loadTweet(new TweetLoadRequest(cursorReader.readSid(c), cursorReader.readQuotedSid(c), (QuotingTweetRowView) rowView, imageLoader, reqWidth));
		}
	},
	SEAMLESS_MEDIA(3, R.layout.tweetlistseamlessmediarow) {
		@Override
		public TweetRowView makeRowView (final View view, final TweetListViewState tweetListViewState) {
			final PendingImage pendingImage = (PendingImage) view.findViewById(R.id.imgMedia);
			pendingImage.setExpandedTracker(tweetListViewState.getExpandedImagesTracker());
			return new TweetRowView(pendingImage);
		}

		@Override
		public void applyTweetTo (final Tweet item, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth, final LinkedTweetLoader tweetLoader) {
			setImage(item.getInlineMediaUrl(), rowView, imageLoader, reqWidth);
		}

		@Override
		public void applyCursorTo (final Cursor c, final TweetCursorReader cursorReader, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth, final LinkedTweetLoader tweetLoader) {
			setImage(cursorReader.readInlineMedia(c), rowView, imageLoader, reqWidth);
		}
	};

	protected static void setImage (final String inlineMediaUrl, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth) {
		final ImageView imageView = rowView.getInlineMedia();
		rowView.showInlineMedia(true);
		if (inlineMediaUrl != null) {
			imageLoader.loadImage(new ImageLoadRequest(inlineMediaUrl, imageView, reqWidth, rowView.getInlineMediaLoadListener()));
		}
		else {
			imageView.setImageResource(R.drawable.question_blue);
		}
	}

	private final int index;
	private final int layout;

	private TweetLayout (final int index, final int layout) {
		this.index = index;
		this.layout = layout;
	}

	public int getIndex () {
		return this.index;
	}

	public int getLayout () {
		return this.layout;
	}

	public abstract TweetRowView makeRowView (final View view, final TweetListViewState tweetListViewState);

	public abstract void applyTweetTo (Tweet item, TweetRowView rowView, ImageLoader imageLoader, int reqWidth, LinkedTweetLoader tweetLoader);

	public abstract void applyCursorTo (Cursor c, TweetCursorReader cursorReader, TweetRowView rowView, ImageLoader imageLoader, int reqWidth, LinkedTweetLoader tweetLoader);

}
