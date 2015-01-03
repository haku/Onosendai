package com.vaguehope.onosendai.provider.hosaka;

import org.json.JSONException;
import org.json.JSONObject;

import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.model.ScrollState;

public class HosakaColumn {

	private final String itemId;
	private final long itemTime;
	private final long unreadTime;

	public HosakaColumn (final String itemId, final long itemTime, final long unreadTime) {
		this.itemId = itemId;
		this.itemTime = itemTime;
		this.unreadTime = unreadTime;
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
		return new HosakaColumn(itemId, itemTime, unreadTime);
	}

	public static String columnHash (final Column column) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented.");
	}

	public ScrollState toScrollState () {
		// FIXME the itemId in SS in _rowid, not sid.
		// Be setting it to -1, should only match on itemTime.
		return new ScrollState(-1, 0, this.itemTime, this.unreadTime);
	}

}
