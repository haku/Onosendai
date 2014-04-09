package com.vaguehope.onosendai.model;

import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.storage.TweetCursorReader;
import com.vaguehope.onosendai.widget.PendingImage;

public enum TweetLayout {

	MAIN(0, R.layout.tweetlistrow) {
		@Override
		public TweetRowView makeRowView (final View view) {
			return new TweetRowView(
					(ImageView) view.findViewById(R.id.imgMain),
					(TextView) view.findViewById(R.id.txtTweet),
					(TextView) view.findViewById(R.id.txtName)
			);
		}

		@Override
		public void applyTweetTo (final Tweet item, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth) {
			rowView.getTweet().setText(item.getBody());
			rowView.getName().setText(item.getUsername() != null ? item.getUsername() : item.getFullname());

			final String avatarUrl = item.getAvatarUrl();
			if (avatarUrl != null) {
				imageLoader.loadImage(new ImageLoadRequest(avatarUrl, rowView.getAvatar()));
			}
			else {
				rowView.getAvatar().setImageResource(R.drawable.question_blue);
			}
		}

		@Override
		public void applyCursorTo (final Cursor c, final TweetCursorReader cursorReader, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth) {
			final String username = cursorReader.readUsername(c);
			final String fullname = cursorReader.readFullname(c);
			final String body = cursorReader.readBody(c);
			final String avatarUrl = cursorReader.readAvatar(c);

			rowView.getTweet().setText(body);
			rowView.getName().setText(username != null ? username : fullname);

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
		public TweetRowView makeRowView (final View view) {
			return new TweetRowView(
					(ImageView) view.findViewById(R.id.imgMain),
					(TextView) view.findViewById(R.id.txtTweet),
					(TextView) view.findViewById(R.id.txtName),
					(PendingImage) view.findViewById(R.id.imgMedia)
			);
		}

		@Override
		public void applyTweetTo (final Tweet item, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth) {
			MAIN.applyTweetTo(item, rowView, imageLoader, reqWidth);
			setImage(item.getInlineMediaUrl(), rowView, imageLoader, reqWidth);
		}

		@Override
		public void applyCursorTo (final Cursor c, final TweetCursorReader cursorReader, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth) {
			MAIN.applyCursorTo(c, cursorReader, rowView, imageLoader, reqWidth);
			setImage(cursorReader.readInlineMedia(c), rowView, imageLoader, reqWidth);
		}
	},
	SEAMLESS_MEDIA(2, R.layout.tweetlistseamlessmediarow) {
		@Override
		public TweetRowView makeRowView (final View view) {
			return new TweetRowView((PendingImage) view.findViewById(R.id.imgMedia));
		}

		@Override
		public void applyTweetTo (final Tweet item, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth) {
			setImage(item.getInlineMediaUrl(), rowView, imageLoader, reqWidth);
		}

		@Override
		public void applyCursorTo (final Cursor c, final TweetCursorReader cursorReader, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth) {
			setImage(cursorReader.readInlineMedia(c), rowView, imageLoader, reqWidth);
		}
	};

	protected static void setImage (final String inlineMediaUrl, final TweetRowView rowView, final ImageLoader imageLoader, final int reqWidth) {
		if (inlineMediaUrl != null) {
			imageLoader.loadImage(new ImageLoadRequest(inlineMediaUrl, rowView.getInlineMedia(), reqWidth, rowView.getInlineMediaLoadListener()));
		}
		else {
			rowView.getInlineMedia().setImageResource(R.drawable.question_blue);
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

	public abstract TweetRowView makeRowView (final View view);

	public abstract void applyTweetTo (Tweet item, TweetRowView rowView, ImageLoader imageLoader, int reqWidth);

	public abstract void applyCursorTo (Cursor c, TweetCursorReader cursorReader, TweetRowView rowView, ImageLoader imageLoader, int reqWidth);

}
