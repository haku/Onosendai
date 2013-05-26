package com.vaguehope.onosendai.ui.pref;

import java.util.List;

import android.preference.PreferenceActivity;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.storage.DbClient;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.LogWrapper;

public class OsPreferenceActivity extends PreferenceActivity {

	private static final LogWrapper LOG = new LogWrapper("PRA");

	@Override
	public void onBuildHeaders (final List<Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
	}

	@Override
	public void onResume () {
		super.onResume();
		resumeDb();
	}

	@Override
	protected void onDestroy () {
		disposeDb();
		super.onDestroy();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private DbClient bndDb;

	private void resumeDb () {
		if (this.bndDb == null) {
			LOG.d("Binding DB service...");
			final LogWrapper log = LOG;
			this.bndDb = new DbClient(this, LOG.getPrefix(), new Runnable() {
				@Override
				public void run () {
					log.d("DB service bound.");
				}
			});
		}
	}

	private void disposeDb () {
		if (this.bndDb != null) this.bndDb.dispose();
	}

	protected DbInterface getDb () {
		final DbClient d = this.bndDb;
		if (d == null) return null;
		return d.getDb();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
