package com.vaguehope.onosendai.ui.pref;

import java.util.List;

import org.acra.ACRA;

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.storage.DbClient;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.LogWrapper;

public class OsPreferenceActivity extends PreferenceActivity {

	private static final LogWrapper LOG = new LogWrapper("PRA");

	@Override
	protected void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final ActionBar ab = getActionBar();
		ab.setDisplayHomeAsUpEnabled(true);

		if (hasHeaders()) {
			final LayoutInflater inflater = LayoutInflater.from(this);
			final Button btn = (Button) inflater.inflate(R.layout.buttonbarbutton, null, false);
			btn.setText("Report a problem"); //ES
			btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick (final View v) {
					ACRA.getErrorReporter().handleException(null);
				}
			});
			setListFooter(btn);
		}
	}

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

	@Override
	protected boolean isValidFragment (final String fragmentName) {
		if (fragmentName == null) return false;
		return fragmentName.startsWith(this.getClass().getPackage().getName());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public boolean onOptionsItemSelected (final MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

}
