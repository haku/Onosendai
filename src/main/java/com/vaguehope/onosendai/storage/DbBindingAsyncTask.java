package com.vaguehope.onosendai.storage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.content.Context;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.util.exec.ExecutorEventListener;
import com.vaguehope.onosendai.util.exec.TrackingAsyncTask;
import com.vaguehope.onosendai.util.LogWrapper;

public abstract class DbBindingAsyncTask<Params, Progress, Result> extends TrackingAsyncTask<Params, Progress, Result> { // NOSONAR Ignore generic names must match pattern '^[A-Z]$' to copy Android SDK.

	private final Context context;
	private final CountDownLatch dbReadyLatch = new CountDownLatch(1);
	private DbClient bndDb;

	public DbBindingAsyncTask (final Context context) {
		this(null, context);
	}

	public DbBindingAsyncTask (final ExecutorEventListener eventListener, final Context context) {
		super(eventListener);
		this.context = context;
	}

	private void connectDb () {
		getLog().d("Binding DB service...");
		final CountDownLatch latch = this.dbReadyLatch;
		this.bndDb = new DbClient(this.context, getLog().getPrefix(), new Runnable() {
			@Override
			public void run () {
				latch.countDown();
				getLog().d("DB service bound.");
			}
		});
	}

	private void disposeDb () {
		if (this.bndDb != null) this.bndDb.dispose();
	}

	private boolean waitForDbReady () {
		boolean dbReady = false;
		try {
			dbReady = this.dbReadyLatch.await(C.DB_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {/**/}
		if (!dbReady) getLog().e("Time out waiting for DB service to connect.");
		return dbReady;
	}

	private DbInterface getDb () {
		final DbClient d = this.bndDb;
		if (d == null) return null;
		return d.getDb();
	}

	protected Context getContext () {
		return this.context;
	}

	@Override
	protected Result doInBackgroundWithTracking (final Params... params) {
		connectDb();
		if (!waitForDbReady()) return null;
		try {
			final DbInterface db = getDb();
			if (db == null) throw new IllegalStateException("DB was not bound.");
			return doInBackgroundWithDb(db, params);
		}
		finally {
			disposeDb();
		}
	}

	protected abstract LogWrapper getLog ();

	protected abstract Result doInBackgroundWithDb (DbInterface db, final Params... params);

}
