package com.vaguehope.onosendai.provider.mastodon;

import java.util.List;

import com.sys1yagi.mastodon4j.api.entity.MastodonList;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.Result;
import com.vaguehope.onosendai.util.DialogHelper.Listener;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public class MastodonListsFetcher extends AsyncTask<Void, Void, Result<List<MastodonList>>> {

	private static final LogWrapper LOG = new LogWrapper("MLF");

	private final Context context;
	private final Account account;
	private final Listener<List<MastodonList>> onLists;

	private ProgressDialog dialog;

	public MastodonListsFetcher (final Context context, final Account account, final Listener<List<MastodonList>> onLists) {
		this.context = context;
		this.account = account;
		this.onLists = onLists;
	}

	@Override
	protected void onPreExecute () {
		this.dialog = ProgressDialog.show(this.context, "Mastodon", "Fetching lists...", true); //ES
	}

	@Override
	protected Result<List<MastodonList>> doInBackground (final Void... params) {
		final MastodonProvider mastodon = new MastodonProvider();
		try {
			return new Result<List<MastodonList>>(mastodon.getLists(this.account));
		}
		catch (final Exception e) { // NOSONAR report all errors to user.
			return new Result<List<MastodonList>>(e);
		}
	}

	@Override
	protected void onPostExecute (final Result<List<MastodonList>> result) {
		this.dialog.dismiss();
		if (result.isSuccess()) {
			this.onLists.onAnswer(result.getData());
		}
		else {
			LOG.e("Failed to fetch lists.", result.getE());
			DialogHelper.alert(this.context, result.getE());
		}
	}

}
