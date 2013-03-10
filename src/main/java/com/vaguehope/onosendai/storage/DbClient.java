package com.vaguehope.onosendai.storage;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.vaguehope.onosendai.util.LogWrapper;

public class DbClient {

	protected final LogWrapper log = new LogWrapper();

	private final Context context;
	protected Runnable dbIsReady = null;
	protected DbInterface mBoundDbService;
	private boolean boundToService = false;


	public DbClient (final Context context, final String name) {
		this(context, name, null);
	}

	public DbClient (final Context context, final String name, final Runnable dbIsReady) {
		this.context = context;
		this.log.setPrefix(name);
		this.dbIsReady = dbIsReady;
		bindDbService();
	}

	public void dispose () {
		unbindDbService();
	}

	@Override
	protected void finalize () {
		unbindDbService();
	}

	public void clearReadyListener () {
		this.dbIsReady = null;
	}

	public DbInterface getDb () {
		return this.mBoundDbService;
	}

	private final ServiceConnection mDbServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected (final ComponentName className, final IBinder service) {
			DbClient.this.mBoundDbService = ((DbService.LocalBinder) service).getService();
			if (DbClient.this.mBoundDbService == null) {
				DbClient.this.log.e("Got service call back, but mBoundDbService==null.  Expect more error messags!");
			}
			if (DbClient.this.dbIsReady != null) DbClient.this.dbIsReady.run();
		}

		@Override
		public void onServiceDisconnected (final ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.

			DbClient.this.mBoundDbService = null;
			DbClient.this.log.w("DbService unexpectadly disconnected.");
		}

	};

	private void bindDbService () {
		this.boundToService = this.context.bindService(new Intent(this.context, DbService.class), this.mDbServiceConnection, Context.BIND_AUTO_CREATE);
		if (!this.boundToService) {
			this.log.e("Failed to bind to DBService.  Expect further nasty errors.");
		}
	}

	private void unbindDbService () {
		if (this.boundToService && this.mBoundDbService != null) {
			try {
				this.context.unbindService(this.mDbServiceConnection);
			}
			catch (Exception e) {
				this.log.e("Exception caught in unbindDbService().", e);
			}
		}
		this.mBoundDbService = null;
	}

}
