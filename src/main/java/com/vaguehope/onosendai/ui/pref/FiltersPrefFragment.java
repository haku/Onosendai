package com.vaguehope.onosendai.ui.pref;

import android.content.Context;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;
import com.vaguehope.onosendai.util.StringHelper;

public class FiltersPrefFragment extends PreferenceFragment {

	private Prefs prefs;

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
		this.prefs = new Prefs(getPreferenceManager());
		refreshFiltersList();
	}

	protected Prefs getPrefs () {
		if (this.prefs == null) throw new IllegalStateException("Prefs has not been initialised.");
		return this.prefs;
	}

	protected void refreshFiltersList () {
		getPreferenceScreen().removeAll();

		final Preference pref = new Preference(getActivity());
		pref.setTitle("Add Filter");
		pref.setSummary("Plain string or regex");
		pref.setOnPreferenceClickListener(new AddFilterClickListener(this));
		getPreferenceScreen().addPreference(pref);

		for (final String filterId : getPrefs().readFilterIds()) {
			final String filter = getPrefs().readFilter(filterId);
			if (StringHelper.isEmpty(filter)) { // FIXME Such hack.  Much lazy.
				getPrefs().deleteFilter(filterId);
			}
			else {
				getPreferenceScreen().addPreference(new FilterPreference(getActivity(), filterId, filter));
			}
		}
	}

	protected void promptNewFilter () {
		DialogHelper.askString(getActivity(), "Start with / for regex.", null, false, false, new Listener<String>() {
			@Override
			public void onAnswer (final String newFilter) {
				final String id = getPrefs().getNextFilterId();
				getPrefs().writeFilter(id, newFilter);
				refreshFiltersList();
			}
		});
	}

	private static class AddFilterClickListener implements OnPreferenceClickListener {

		private final FiltersPrefFragment host;

		public AddFilterClickListener (final FiltersPrefFragment host) {
			this.host = host;
		}

		@Override
		public boolean onPreferenceClick (final Preference preference) {
			this.host.promptNewFilter();
			return true;
		}
	}

	private static class FilterPreference extends EditTextPreference {

		public FilterPreference (final Context context, final String filterId, final String initialValue) {
			super(context);
			setKey(filterId);
			setTitle(initialValue);
			setDialogMessage("Start with / for regex.  Save empty string to delete.");

			final EditText editText = getEditText();
			editText.setSingleLine();
			editText.setInputType(editText.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
		}

		@Override
		public void setText (final String text) {
			super.setText(text);
			setTitle(text);
		}

	}

}
