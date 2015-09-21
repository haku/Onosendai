package com.vaguehope.onosendai.model;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import com.vaguehope.onosendai.config.InlineMediaStyle;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.storage.TweetCursorReader;

public class TweetListCursorAdapter extends CursorAdapter {

	private final InlineMediaStyle inlineMediaStyle;
	private final ImageLoader imageLoader;
	private final LinkedTweetLoader tweetLoader;
	private final View listView;
	private final LayoutInflater layoutInflater;

	private final TweetCursorReader cursorReader = new TweetCursorReader();
	private final TweetListViewState tweetListViewState = new TweetListViewState();

	public TweetListCursorAdapter (final Context context, final InlineMediaStyle inlineMediaStyle, final ImageLoader imageLoader, final LinkedTweetLoader tweetLoader, final View listView) {
		super(context, null, false); // Initialise with no cursor.
		this.inlineMediaStyle = inlineMediaStyle;
		this.imageLoader = imageLoader;
		this.tweetLoader = tweetLoader;
		this.listView = listView;
		this.layoutInflater = LayoutInflater.from(context);
	}

	public void dispose () {
		changeCursor(null);
	}

	public String getItemSid (final int position) {
		return this.cursorReader.readSid((Cursor) getItem(position));
	}

	public long getItemTime (final int position) {
		if (getCount() < 1) return -1;
		return this.cursorReader.readTime((Cursor) getItem(position));
	}

	@Override
	public int getViewTypeCount () {
		return TweetLayout.values().length;
	}

	@Override
	public int getItemViewType (final int position) {
		return tweetLayoutType((Cursor) getItem(position)).getIndex();
	}

	@Override
	public View newView (final Context context, final Cursor cursor, final ViewGroup parent) {
		final TweetLayout layoutType = tweetLayoutType(cursor);
		final View view = this.layoutInflater.inflate(layoutType.getLayout(), null);
		final TweetRowView rowView = layoutType.makeRowView(view, this.tweetListViewState);
		view.setTag(rowView);
		return view;
	}

	@Override
	public void bindView (final View view, final Context context, final Cursor cursor) {
		final TweetLayout layoutType = tweetLayoutType(cursor);
		final TweetRowView rowView = (TweetRowView) view.getTag();
		layoutType.applyCursorTo(cursor, this.cursorReader, rowView, this.imageLoader, this.listView.getWidth(), this.tweetLoader);
	}

	private TweetLayout tweetLayoutType (final Cursor c) {
		switch (this.inlineMediaStyle) {
			case NONE:
				// TODO inline quote without media?
				return TweetLayout.MAIN;
			case INLINE:
				if (this.cursorReader.readQuotedSid(c) != null) return TweetLayout.QUOTED;
				return this.cursorReader.readInlineMedia(c) != null ? TweetLayout.INLINE_MEDIA : TweetLayout.MAIN;
			case SEAMLESS:
				return this.cursorReader.readInlineMedia(c) != null ? TweetLayout.SEAMLESS_MEDIA : TweetLayout.MAIN;
			default:
				return TweetLayout.MAIN;
		}
	}

}
