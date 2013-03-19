package com.vaguehope.onosendai.model;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class MetaBuilder {

	static final String KEY_TYPE = "T";
	static final String KEY_DATA = "D";

	private JSONArray arr = null;

	public MetaBuilder () {}

	public void reset () {
		this.arr = null;
	}

	private void init () {
		if (this.arr == null) this.arr = new JSONArray();
	}

	public void add (final MetaType type, final String data) {
		init();
		try {
			final JSONObject obj = new JSONObject();
			obj.put(KEY_TYPE, type.getKey());
			obj.put(KEY_DATA, data);
			this.arr.put(obj);
		}
		catch (final JSONException e) {
			throw new IllegalStateException(e);
		}
	}

	public String build () {
		if (this.arr == null) return null;
		final String ret = this.arr.toString();
		reset();
		return ret;
	}

	public static List<Meta> parseMeta(final String json) throws JSONException {
		List<Meta> ret = new ArrayList<Meta>();
		JSONArray arr = (JSONArray) new JSONTokener(json).nextValue();
		for (int i = 0; i < arr.length(); i++) {
			JSONObject obj = arr.getJSONObject(i);
			MetaType type = MetaType.parseKey(obj.getString(MetaBuilder.KEY_TYPE));
			String data = obj.getString(MetaBuilder.KEY_DATA);
			ret.add(new Meta(type, data));
		}
		return ret;
	}

}
