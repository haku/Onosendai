package com.vaguehope.onosendai.storage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.util.LogWrapper;

public abstract class DbBindingService extends IntentService {

	private final LogWrapper log;
	private final CountDownLatch dbReadyLatch = new CountDownLatch(1);
	private DbClient bndDb;

	public DbBindingService (final String name, final LogWrapper log) {
		super(name);
		this.log = log;
	}

	@Override
	public void onCreate () {
		super.onCreate();
		connectDb();
	}

	@Override
	public void onDestroy () {
		disconnectDb();
		super.onDestroy();
	}

	@Override
	protected void onHandleIntent (final Intent i) {
		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		final WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, C.TAG);
		wl.acquire();
		try {
			doWork(i);
		}
		finally {
			wl.release();
		}
	}

	protected abstract void doWork (Intent i);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void connectDb () {
		this.log.d("Binding DB service...");
		final CountDownLatch latch = this.dbReadyLatch;
		this.bndDb = new DbClient(getApplicationContext(), this.log.getPrefix(), new Runnable() {
			@Override
			public void run () {
				latch.countDown();
				getLog().d("DB service bound.");
			}
		});
	}

	private void disconnectDb () {
		this.bndDb.dispose();
		this.log.d("DB service rebound.");
	}

	protected boolean waitForDbReady () {
		boolean dbReady = false;
		try {
			dbReady = this.dbReadyLatch.await(C.DB_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		}
		catch (final InterruptedException e) {/**/}
		if (!dbReady) this.log.e("Aborting: Time out waiting for DB service to connect.");
		return dbReady;
	}

	protected DbInterface getDb () {
		final DbClient d = this.bndDb;
		if (d == null) return null;
		return d.getDb();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected LogWrapper getLog () {
		return this.log;
	}

}
