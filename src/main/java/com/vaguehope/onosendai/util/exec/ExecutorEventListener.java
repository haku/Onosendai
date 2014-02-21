package com.vaguehope.onosendai.util.exec;

import android.os.AsyncTask;

public interface ExecutorEventListener {

	void execStart (AsyncTask<?, ?, ?> task);

	void execEnd (AsyncTask<?, ?, ?> task);

	void execStart (Runnable command);

	void execEnd (Runnable command);

}
