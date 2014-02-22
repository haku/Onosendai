package com.vaguehope.onosendai.ui;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.widget.TextView;

import com.vaguehope.onosendai.util.exec.ExecutorEventListener;

public class ExecutorStatus implements ExecutorEventListener {

	private final TextView textView;
	private final RefreshUiHandler refreshUiHandler;

	private final Map<Thread, String> threads = new ConcurrentSkipListMap<Thread, String>(new Comparator<Thread>() {
		@Override
		public int compare (final Thread lhs, final Thread rhs) {
			final long thisVal = lhs.getId();
			final long anotherVal = rhs.getId();
			return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
		}
	});

	public ExecutorStatus (final TextView textView) {
		this.textView = textView;
		textView.setHorizontallyScrolling(true);
		textView.setEllipsize(null);
		this.refreshUiHandler = new RefreshUiHandler(this);
	}

	@Override
	public void execStart (final Runnable command) {
		this.threads.put(Thread.currentThread(), "[task]");
		this.refreshUiHandler.sendEmptyMessage(0);
	}

	@Override
	public void execStart (final AsyncTask<?, ?, ?> task) {
		this.threads.put(Thread.currentThread(), task.toString());
		this.refreshUiHandler.sendEmptyMessage(0);
	}

	@Override
	public void execEnd (final AsyncTask<?, ?, ?> task) {
		// Unused.
	}

	@Override
	public void execEnd (final Runnable command) {
		this.threads.put(Thread.currentThread(), "idle.");
		this.refreshUiHandler.sendEmptyMessage(1);
	}

	private static class RefreshUiHandler extends Handler {

		private final WeakReference<ExecutorStatus> parentRef;

		public RefreshUiHandler (final ExecutorStatus parent) {
			this.parentRef = new WeakReference<ExecutorStatus>(parent);
		}

		@Override
		public void handleMessage (final Message msg) {
			final ExecutorStatus parent = this.parentRef.get();
			if (parent != null) parent.msgOnUiThread(msg);
		}

	}

	private long lastUpdate = 0L;
	private boolean sentReminder = false;

	protected void msgOnUiThread (final Message msg) {
		if (SystemClock.uptimeMillis() - this.lastUpdate < 200) {
			if (!this.sentReminder) {
				this.refreshUiHandler.sendEmptyMessageDelayed(2, 500);
				this.sentReminder = true;
			}
			return;
		}

		cleapThreads();
		redraw();

		this.lastUpdate = SystemClock.uptimeMillis();
		this.sentReminder = false;
	}

	private void cleapThreads () {
		final Iterator<Entry<Thread, String>> i = this.threads.entrySet().iterator();
		while (i.hasNext()) {
			if (!i.next().getKey().isAlive()) i.remove();
		}
	}

	private void redraw () {
		final StringBuilder s = new StringBuilder();
		for (final Entry<Thread, String> e : this.threads.entrySet()) {
			if (s.length() > 0) s.append("\n");
			s.append(e.getKey().getId()).append(" ").append(e.getValue());
		}
		if (s.length() < 1) s.append("idle.");
		this.textView.setText(s);
	}

}
