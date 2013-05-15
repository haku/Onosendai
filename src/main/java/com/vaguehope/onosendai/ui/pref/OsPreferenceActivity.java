package com.vaguehope.onosendai.ui.pref;

import java.util.List;

import android.preference.PreferenceActivity;

import com.vaguehope.onosendai.R;

public class OsPreferenceActivity extends PreferenceActivity {

	@Override
	public void onBuildHeaders (final List<Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
	}

}
