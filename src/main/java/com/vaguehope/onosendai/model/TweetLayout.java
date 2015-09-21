package com.vaguehope.onosendai.model;

import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.model.TweetRowView.QuotingTweetRowView;
import com.vaguehope.onosendai.storage.DbProvider;
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
		public void applyTweetTo (final Tweet item, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth, final DbProvider dbProvider) {
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
		public void applyCursorTo (final Cursor c, final TweetCursorReader cursorReader, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth, final DbProvider dbProvider) {
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
		public void applyTweetTo (final Tweet item, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth, final DbProvider dbProvider) {
			MAIN.applyTweetTo(item, rowView, imageLoader, reqWidth, dbProvider);
			setImage(item.getInlineMediaUrl(), rowView, imageLoader, reqWidth);
		}

		@Override
		public void applyCursorTo (final Cursor c, final TweetCursorReader cursorReader, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth, final DbProvider dbProvider) {
			MAIN.applyCursorTo(c, cursorReader, rowView, imageLoader, reqWidth, dbProvider);
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
		public void applyTweetTo (final Tweet item, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth, final DbProvider dbProvider) {
			MAIN.applyTweetTo(item, rowView, imageLoader, reqWidth, dbProvider);
			final String inlineMediaUrl = item.getInlineMediaUrl();
			if (inlineMediaUrl != null) {
				setImage(inlineMediaUrl, rowView, imageLoader, reqWidth);
			}
			else {
				rowView.showInlineMedia(false);
			}
			applyQuotedTweet(item.getQuotedSid(), dbProvider, (QuotingTweetRowView) rowView, imageLoader, reqWidth);
		}

		@Override
		public void applyCursorTo (final Cursor c, final TweetCursorReader cursorReader, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth, final DbProvider dbProvider) {
			MAIN.applyCursorTo(c, cursorReader, rowView, imageLoader, reqWidth, dbProvider);
			final String inlineMediaUrl = cursorReader.readInlineMedia(c);
			if (inlineMediaUrl != null) {
				setImage(inlineMediaUrl, rowView, imageLoader, reqWidth);
			}
			else {
				rowView.showInlineMedia(false);
			}
			applyQuotedTweet(cursorReader.readQuotedSid(c), dbProvider, (QuotingTweetRowView) rowView, imageLoader, reqWidth);
		}

		private void applyQuotedTweet (final String quotedSid, final DbProvider dbProvider, final QuotingTweetRowView rowView, final ImageLoader imageLoader, final int reqWidth) {
			// TODO load quoted tweet on BG thread.
			final Tweet quotedTweet = dbProvider.getDb().getTweetDetails(quotedSid);
			if (quotedTweet != null) {
				rowView.getQTweet().setText(quotedTweet.getBody());

				final String usernameWithSubtitle = quotedTweet.getUsernameWithSubtitle();
				rowView.getQName().setText(usernameWithSubtitle != null ? usernameWithSubtitle : quotedTweet.getFullnameWithSubtitle());

				final String avatarUrl = quotedTweet.getAvatarUrl();
				if (avatarUrl != null) {
					imageLoader.loadImage(new ImageLoadRequest(avatarUrl, rowView.getQAvatar()));
				}
				else {
					rowView.getQAvatar().setImageResource(R.drawable.question_blue);
				}

				final String quotedInlineMediaUrl = quotedTweet.getInlineMediaUrl();
				if (quotedInlineMediaUrl != null) {
					rowView.showQInlineMedia(true);
					imageLoader.loadImage(new ImageLoadRequest(quotedInlineMediaUrl, rowView.getQInlineMedia(), reqWidth, rowView.getQInlineMediaLoadListener()));
				}
				else {
					rowView.showQInlineMedia(false);
				}
			}
			else {
				rowView.getQTweet().setText(String.format("[ %s ]", quotedSid));
				rowView.getQName().setText("");
				rowView.getQAvatar().setImageResource(R.drawable.question_blue);
				rowView.showQInlineMedia(false);
			}

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
		public void applyTweetTo (final Tweet item, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth, final DbProvider dbProvider) {
			setImage(item.getInlineMediaUrl(), rowView, imageLoader, reqWidth);
		}

		@Override
		public void applyCursorTo (final Cursor c, final TweetCursorReader cursorReader, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth, final DbProvider dbProvider) {
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

	public abstract void applyTweetTo (Tweet item, TweetRowView rowView, ImageLoader imageLoader, int reqWidth, DbProvider dbProvider);

	public abstract void applyCursorTo (Cursor c, TweetCursorReader cursorReader, TweetRowView rowView, ImageLoader imageLoader, int reqWidth, DbProvider dbProvider);

}
