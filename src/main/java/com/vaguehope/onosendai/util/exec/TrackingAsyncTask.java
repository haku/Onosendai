package com.vaguehope.onosendai.util.exec;

import android.os.AsyncTask;

public abstract class TrackingAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> { // NOSONAR Ignore generic names must match pattern '^[A-Z]$' to copy Android SDK.

	private final ExecutorEventListener eventListener;

	public TrackingAsyncTask (final ExecutorEventListener eventListener) {
		super();
		this.eventListener = eventListener;
	}

	public ExecutorEventListener getEventListener () {
		return this.eventListener;
	}

	@Override
	protected final Result doInBackground (final Params... params) {
		if (this.eventListener != null) this.eventListener.execStart(this);
		try {
			return this.doInBackgroundWithTracking(params);
		}
		finally {
			if (this.eventListener != null) this.eventListener.execEnd(this);
		}
	}

	protected abstract Result doInBackgroundWithTracking (final Params... params);

}
