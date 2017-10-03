package com.vaguehope.onosendai.config;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.vaguehope.onosendai.util.ArrayHelper;
import com.vaguehope.onosendai.util.EqualHelper;
import com.vaguehope.onosendai.util.Titleable;

public class NotificationStyle implements Titleable {

	public static final NotificationStyle DEFAULT = new NotificationStyle(false, false, false, false);

	private static final String KEY_LIGHTS = "lights";
	private static final String KEY_VIBRATE = "vibrate";
	private static final String KEY_SOUND = "sound";
	private static final String KEY_EXCLUDE_RETWEETS = "exclude_retweets";

	private final boolean lights;
	private final boolean vibrate;
	private final boolean sound;
	private final boolean excludeRetweets;

	private String title;

	public NotificationStyle (final boolean lights, final boolean vibrate, final boolean sound, final boolean excludeRetweets) {
		this.lights = lights;
		this.vibrate = vibrate;
		this.sound = sound;
		this.excludeRetweets = excludeRetweets;
	}

	@Override
	public String getUiTitle () {
		if (this.title == null) {
			final List<String> l = new ArrayList<String>();
			if (isLights()) l.add("lights"); //ES
			if (isVibrate()) l.add("vibrate"); //ES
			if (isSound()) l.add("sound"); //ES
			if (isExcludeRetweets()) l.add("exclude retweets"); //ES
			if (l.size() > 0) {
				this.title = ArrayHelper.join(l, ", ");
			}
			else {
				this.title = "plain"; //ES
			}
		}
		return this.title;
	}

	public boolean isLights () {
		return this.lights;
	}

	public boolean isVibrate () {
		return this.vibrate;
	}

	public boolean isSound () {
		return this.sound;
	}

	public boolean isExcludeRetweets () {
		return this.excludeRetweets;
	}

	public JSONObject toJson () throws JSONException {
		final JSONObject json = new JSONObject();
		json.put(KEY_LIGHTS, isLights());
		json.put(KEY_VIBRATE, isVibrate());
		json.put(KEY_SOUND, isSound());
		json.put(KEY_EXCLUDE_RETWEETS, isExcludeRetweets());
		return json;
	}

	@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.lights ? Boolean.TRUE.hashCode() : Boolean.FALSE.hashCode());
		result = prime * result + (this.vibrate ? Boolean.TRUE.hashCode() : Boolean.FALSE.hashCode());
		result = prime * result + (this.sound ? Boolean.TRUE.hashCode() : Boolean.FALSE.hashCode());
		result = prime * result + (this.excludeRetweets ? Boolean.TRUE.hashCode() : Boolean.FALSE.hashCode());
		return result;
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof NotificationStyle)) return false;
		final NotificationStyle that = (NotificationStyle) o;
		return EqualHelper.equal(this.lights, that.lights) &&
				EqualHelper.equal(this.vibrate, that.vibrate) &&
				EqualHelper.equal(this.sound, that.sound) &&
				EqualHelper.equal(this.excludeRetweets, that.excludeRetweets);
	}

	@Override
	public String toString () {
		return new StringBuilder().append("NotificationStyle{")
				.append(",").append(this.lights)
				.append(",").append(this.vibrate)
				.append(",").append(this.sound)
				.append(",").append(this.excludeRetweets)
				.append("}").toString();
	}

	public static NotificationStyle parseJson (final Object obj) throws JSONException {
		if (obj == null) return null;
		if (obj instanceof String) return parseJson((String) obj);
		if (obj instanceof JSONObject) return parseJson((JSONObject) obj);
		if (obj instanceof Boolean) {
			final Boolean b = (Boolean) obj;
			return b.booleanValue() ? DEFAULT : null;
		}
		throw new IllegalArgumentException("Unexpected object type " + obj.getClass() + ": " + obj);
	}

	public static NotificationStyle parseJson (final String json) throws JSONException {
		if (json == null) return null;
		return parseJson((JSONObject) new JSONTokener(json).nextValue());
	}

	public static NotificationStyle parseJson (final JSONObject json) throws JSONException {
		if (json == null) throw new IllegalArgumentException("json can not be null.");
		return new NotificationStyle(
				json.getBoolean(KEY_LIGHTS),
				json.getBoolean(KEY_VIBRATE),
				json.getBoolean(KEY_SOUND),
				json.optBoolean(KEY_EXCLUDE_RETWEETS, DEFAULT.isExcludeRetweets()));
	}

}
