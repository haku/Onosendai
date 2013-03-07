package com.vaguehope.onosendai.storage;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.vaguehope.onosendai.C;

public class DbClient {

	protected Runnable dbIsReady = null;
	protected DbInterface mBoundDbService;
	private Context context;
	private boolean boundToService = false;

	public DbClient (Context context) {
		this(context, null);
	}

	public DbClient (Context context, Runnable dbIsReady) {
		this.context = context;
		this.dbIsReady = dbIsReady;
		bindDbService();
	}

	@Override
	public void finalize () {
		unbindDbService();
	}

	public void clearReadyListener () {
		this.dbIsReady = null;
	}

	public DbInterface getDb () {
		return this.mBoundDbService;
	}

	private ServiceConnection mDbServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected (ComponentName className, IBinder service) {
			DbClient.this.mBoundDbService = ((DbService.LocalBinder) service).getService();
			if (DbClient.this.mBoundDbService == null) Log.e(C.TAG, "Got service call back, but mBoundDbService==null.  Expect more error messags!");
			if (DbClient.this.dbIsReady != null) DbClient.this.dbIsReady.run();
		}

		@Override
		public void onServiceDisconnected (ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.

			DbClient.this.mBoundDbService = null;
			Log.w(C.TAG, "DbService unexpectadly disconnected.");
		}

	};

	private void bindDbService () {
		this.boundToService = this.context.bindService(new Intent(this.context, DbService.class), this.mDbServiceConnection, Context.BIND_AUTO_CREATE);
		if (!this.boundToService) {
			Log.e(C.TAG, "Failed to bind to DBService.  Expect further nasty errors.");
		}
	}

	private void unbindDbService () {
		if (this.boundToService && this.mBoundDbService != null) {
			try {
				this.context.unbindService(this.mDbServiceConnection);
			}
			catch (Exception e) {
				Log.e(C.TAG, "Exception caught in unbindDbService().", e);
			}
		}
		this.mBoundDbService = null;
	}

}
