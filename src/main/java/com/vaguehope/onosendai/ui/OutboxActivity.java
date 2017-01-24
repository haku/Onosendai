package com.vaguehope.onosendai.ui;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.model.OutboxAdapter;
import com.vaguehope.onosendai.model.OutboxTweet;
import com.vaguehope.onosendai.model.OutboxTweet.OutboxTweetStatus;
import com.vaguehope.onosendai.provider.SendOutboxService;
import com.vaguehope.onosendai.storage.DbClient;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.storage.DbInterface.OutboxListener;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.Titleable;

public class OutboxActivity extends Activity {

	private static final LogWrapper LOG = new LogWrapper("OUTBOX");

	private DbClient bndDb;
	private RefreshUiHandler refreshUiHandler;
	private OutboxAdapter adaptor;

	@Override
	protected void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.outbox);

		Config conf;
		try {
			final Prefs prefs = new Prefs(getBaseContext());
			conf = prefs.asConfig();
		}
		catch (final Exception e) { // No point continuing if any exception.
			DialogHelper.alertAndClose(this, e);
			return;
		}

		this.refreshUiHandler = new RefreshUiHandler(this);

		final ListView outboxList = (ListView) findViewById(R.id.outboxList);
		outboxList.setEmptyView(findViewById(R.id.empty));
		this.adaptor = new OutboxAdapter(this, conf);
		outboxList.setAdapter(this.adaptor);
		outboxList.setOnItemClickListener(this.listClickListener);

		((Button) findViewById(R.id.resetPermanentFailures)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (final View v) {
				resetPermanentFailures();
			}
		});
		((Button) findViewById(R.id.sendPending)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (final View v) {
				scheduleSend();
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

	protected OutboxAdapter getAdaptor () {
		return this.adaptor;
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
			final List<OutboxTweet> entries = db.getUnsentOutboxEntries();
			this.adaptor.setInputData(entries);
		}
		else {
			LOG.w("Failed to refresh outbox as DB was not bound.");
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final OnItemClickListener listClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick (final AdapterView<?> parent, final View view, final int position, final long id) {
			itemClicked(getAdaptor().getInputData().get(position));
		}
	};

	private enum OutboxItemAction implements Titleable {
		VIEW_ERROR("View Error") { //ES
			@Override
			public void onClick (final OutboxActivity oa, final OutboxTweet ot) {
				DialogHelper.alert(oa, ot.getLastError());
			}
		},
		EDIT_AS_NEW("Edit") { //ES
			@Override
			public void onClick (final OutboxActivity oa, final OutboxTweet ot) {
				oa.getDb().updateOutboxEntry(ot.setPaused());
				oa.startActivity(new Intent(oa, PostActivity.class)
						.putExtra(PostActivity.ARG_ACCOUNT_ID, ot.getAccountId())
						.putStringArrayListExtra(PostActivity.ARG_SVCS, new ArrayList<String>(ot.getSvcMetasList()))
						.putExtra(PostActivity.ARG_IN_REPLY_TO_SID, ot.getInReplyToSid())
						// TODO currently these are not saved in the outbox.
						// Not specifying these means the preview will not be displayed.
						// Everything else should work as expected.  Hopefully.
						// - ARG_IN_REPLY_TO_UID
						// - ARG_ALT_REPLY_TO_SID
						.putExtra(PostActivity.ARG_BODY, ot.getBody())
						.putExtra(PostActivity.ARG_ATTACHMENT, ot.getAttachment())
						.putExtra(PostActivity.ARG_OUTBOX_UID, ot.getUid().longValue()));
			}
		},
		COPY_BODY("Copy Body") { //ES
			@Override
			public void onClick (final OutboxActivity oa, final OutboxTweet ot) {
				((ClipboardManager) oa.getSystemService(Context.CLIPBOARD_SERVICE))
						.setPrimaryClip(ClipData.newPlainText("Tweet", ot.getBody())); //ES
			}
		},
		COPY_ERROR("Copy Error") { //ES
			@Override
			public void onClick (final OutboxActivity oa, final OutboxTweet ot) {
				((ClipboardManager) oa.getSystemService(Context.CLIPBOARD_SERVICE))
						.setPrimaryClip(ClipData.newPlainText("Error Message", ot.getLastError())); //ES
			}
		},
		DELETE("Delete") { //ES
			@Override
			public void onClick (final OutboxActivity oa, final OutboxTweet ot) {
				DialogHelper.askYesNo(oa, "Delete outbox item?", "Delete", "Keep", new Runnable() { //ES
					@Override
					public void run () {
						oa.getDb().deleteFromOutbox(ot);
					}
				});
			}
		};

		private final String title;

		private OutboxItemAction (final String title) {
			this.title = title;
		}

		@Override
		public String getUiTitle () {
			return this.title;
		}

		public abstract void onClick (OutboxActivity oa, OutboxTweet ot);
	}

	protected void itemClicked (final OutboxTweet ot) {
		DialogHelper.askItem(this, "Outbox Item", OutboxItemAction.values(), new Listener<OutboxItemAction>() { //ES
			@Override
			public void onAnswer (final OutboxItemAction answer) {
				answer.onClick(OutboxActivity.this, ot);
			}
		});
	}

	protected void resetPermanentFailures () {
		for (final OutboxTweet ot : getDb().getOutboxEntries(OutboxTweetStatus.PERMANENTLY_FAILED)) {
			getDb().updateOutboxEntry(ot.setPending());
		}
	}

	protected void scheduleSend () {
		startService(new Intent(this, SendOutboxService.class));
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

}
