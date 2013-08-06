package com.vaguehope.onosendai.ui;

import java.lang.ref.WeakReference;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.model.OutboxTweet;
import com.vaguehope.onosendai.provider.SendOutboxService;
import com.vaguehope.onosendai.storage.DbClient;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.storage.DbInterface.OutboxListener;
import com.vaguehope.onosendai.util.LogWrapper;

public class OutboxActivity extends Activity {

	private static final LogWrapper LOG = new LogWrapper("OUTBOX");

	private DbClient bndDb;
	private RefreshUiHandler refreshUiHandler;
	private ArrayAdapter<OutboxTweet> adaptor;

	@Override
	protected void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.outbox);

		this.refreshUiHandler = new RefreshUiHandler(this);

		final ListView outboxList = (ListView) findViewById(R.id.outboxList);

		this.adaptor = new ArrayAdapter<OutboxTweet>(this, R.layout.numberspinneritem); // TODO own layout.
		outboxList.setAdapter(this.adaptor);

		((Button) findViewById(R.id.syncOutbox)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (final View v) {
				scheduleSync();
			}
		});
	}

	@Override
	public void onDestroy () {
		if (this.bndDb != null) this.bndDb.dispose();
		super.onDestroy();
	}

	@Override
	public void onResume () {
		super.onResume();
		resumeDb();
	}

	@Override
	public void onPause () {
		suspendDb();
		super.onPause();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected LogWrapper getLog () {
		return LOG;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void resumeDb () {
		if (this.bndDb == null) {
			LOG.d("Binding DB service...");
			this.bndDb = new DbClient(this, LOG.getPrefix(), new Runnable() {
				@Override
				public void run () {
					/*
					 * this convoluted method is because the service connection
					 * won't finish until this thread processes messages again
					 * (i.e., after it exits this thread). if we try to talk to
					 * the DB service before then, it will NPE.
					 */
					getDb().addOutboxListener(getOutboxListener());
					refreshUi();
					getLog().d("DB service bound.");
				}
			});
		}
		else { // because we stop listening in onPause(), we must resume if the user comes back.
			this.bndDb.getDb().addOutboxListener(getOutboxListener());
			refreshUi();
			LOG.d("DB service rebound.");
		}
	}

	private void suspendDb () {
		// We might be pausing before the callback has come.
		if (this.bndDb.getDb() != null) {
			this.bndDb.getDb().removeOutboxListener(getOutboxListener());
		}
		else {
			// If we have not even had the callback yet, cancel it.
			this.bndDb.clearReadyListener();
		}
		LOG.d("DB service released.");
	}

	protected DbInterface getDb () {
		final DbClient d = this.bndDb;
		if (d == null) return null;
		return d.getDb();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected OutboxListener getOutboxListener () {
		return this.outboxListener;
	}

	private final OutboxListener outboxListener = new OutboxListener() {
		@Override
		public void outboxChanged () {
			refreshUi();
		}
	};

	protected void refreshUi () {
		this.refreshUiHandler.sendEmptyMessage(0);
	}

	private static class RefreshUiHandler extends Handler {

		private final WeakReference<OutboxActivity> parentRef;

		public RefreshUiHandler (final OutboxActivity parent) {
			this.parentRef = new WeakReference<OutboxActivity>(parent);
		}

		@Override
		public void handleMessage (final Message msg) {
			final OutboxActivity parent = this.parentRef.get();
			if (parent != null) parent.msgOnUiThread(msg);
		}
	}

	protected void msgOnUiThread (final Message msg) {
		refreshListOnUiThread();
	}

	private void refreshListOnUiThread () {
		final DbInterface db = getDb();
		if (db != null) {
			final List<OutboxTweet> entries = db.getOutboxEntries();
			this.adaptor.clear();
			this.adaptor.addAll(entries);
		}
		else {
			LOG.w("Failed to refresh outbox as DB was not bound.");
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected void scheduleSync() {
		startService(new Intent(this, SendOutboxService.class));
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

}
