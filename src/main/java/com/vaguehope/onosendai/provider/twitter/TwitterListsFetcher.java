package com.vaguehope.onosendai.provider.twitter;

import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.Result;

public class TwitterListsFetcher extends AsyncTask<Void, Void, Result<List<String>>> {

	private static final LogWrapper LOG = new LogWrapper("TLF");

	private final Activity activity;
	private final Account account;
	private final Listener<List<String>> onLists;

	private ProgressDialog dialog;

	public TwitterListsFetcher (final Activity host, final Account account, final Listener<List<String>> onLists) {
		this.activity = host;
		this.account = account;
		this.onLists = onLists;
	}

	@Override
	protected void onPreExecute () {
		this.dialog = ProgressDialog.show(this.activity, "Twitter", "Fetching lists...", true);
	}

	@Override
	protected Result<List<String>> doInBackground (final Void... params) {
		final TwitterProvider twitter = new TwitterProvider();
		try {
			return new Result<List<String>>(twitter.getListSlugs(this.account));
		}
		catch (final Exception e) { // NOSONAR report all errors to user.
			return new Result<List<String>>(e);
		}
		finally {
			twitter.shutdown();
		}
	}

	@Override
	protected void onPostExecute (final Result<List<String>> result) {
		this.dialog.dismiss();
		if (result.isSuccess()) {
			this.onLists.onAnswer(result.getData());
		}
		else {
			LOG.e("Failed to fetch Lists.", result.getE());
			DialogHelper.alert(this.activity, result.getE());
		}
	}

}