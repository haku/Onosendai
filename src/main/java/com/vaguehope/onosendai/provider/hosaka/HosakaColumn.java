package com.vaguehope.onosendai.provider.hosaka;

import java.util.Arrays;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.ColumnFeed;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.model.ScrollState.ScrollDirection;
import com.vaguehope.onosendai.util.ArrayHelper;
import com.vaguehope.onosendai.util.HashHelper;

public class HosakaColumn {

	private final String itemId;
	private final long itemTime;
	private final long unreadTime;
	private final ScrollDirection scrollDirection;

	public HosakaColumn (final String itemId, final long itemTime, final long unreadTime, final ScrollDirection scrollDirection) {
		this.itemId = itemId;
		this.itemTime = itemTime;
		this.unreadTime = unreadTime;
		this.scrollDirection = scrollDirection;
	}

	public String getItemId () {
		return this.itemId;
	}

	public long getItemTime () {
		return this.itemTime;
	}

	public long getUnreadTime () {
		return this.unreadTime;
	}

	public ScrollDirection getScrollDirection () {
		return this.scrollDirection;
	}

	@Override
	public String toString () {
		return new StringBuilder()
				.append("HosakaColumn{").append(this.itemId)
				.append(',').append(this.itemTime)
				.append(',').append(this.unreadTime)
				.append(',').append(this.scrollDirection)
				.append('}')
				.toString();
	}

	public JSONObject toJson () throws JSONException {
		final JSONObject json = new JSONObject();
		if (this.itemId != null) json.put("item_id", this.itemId);
		if (this.itemTime > 0L) json.put("item_time", this.itemTime);
		if (this.unreadTime > 0L) json.put("unread_time", this.unreadTime);
		return json;
	}

	public static HosakaColumn parseJson (final JSONObject json) throws JSONException {
		// TODO validation?
		final String itemId = json.getString("item_id");
		final long itemTime = json.getLong("item_time");
		final long unreadTime = json.getLong("unread_time");
		return new HosakaColumn(itemId, itemTime, unreadTime, null);
	}

	public static String columnHash (final Column col, final Config conf) {
		final String[] arr = new String[col.getFeeds().size()];
		final Iterator<ColumnFeed> feedsItr = col.getFeeds().iterator();
		for (int i = 0; feedsItr.hasNext(); i++) {
			final ColumnFeed cf = feedsItr.next();
			final Account act = conf.getAccount(cf.getAccountId());
			arr[i] = String.format("%s:%s", act != null ? act.getTitle() : null, cf.getResource());
		}
		Arrays.sort(arr);
		return HashHelper.sha1String(ArrayHelper.join(arr, ":")).toString(16);
	}

	public ScrollState toScrollState () {
		// FIXME the itemId in SS in _rowid, not sid.
		// Be setting it to -1, should only match on itemTime.
		return new ScrollState(-1, 0, this.itemTime, this.unreadTime, this.scrollDirection);
	}

}
