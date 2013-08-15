package com.vaguehope.onosendai.provider.successwhale;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.storage.VolatileKvStore;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.Result;

public class SuccessWhaleColumnsFetcher extends AsyncTask<Void, Void, Result<SuccessWhaleColumns>> {

	private static final LogWrapper LOG = new LogWrapper("SWCF");

	private final Context context;
	private final Account account;
	private final Listener<SuccessWhaleColumns> onColumns;

	private ProgressDialog dialog;

	public SuccessWhaleColumnsFetcher (final Context context, final Account account, final Listener<SuccessWhaleColumns> onColumns) {
		this.context = context;
		this.account = account;
		this.onColumns = onColumns;
	}

	@Override
	protected void onPreExecute () {
		this.dialog = ProgressDialog.show(this.context, "SuccessWhale", "Fetching columns...", true);
	}

	@Override
	protected Result<SuccessWhaleColumns> doInBackground (final Void... params) {
		final SuccessWhaleProvider swProv = new SuccessWhaleProvider(new VolatileKvStore());
		try {
			return new Result<SuccessWhaleColumns>(swProv.getColumns(this.account));
		}
		catch (SuccessWhaleException e) {
			return new Result<SuccessWhaleColumns>(e);
		}
		finally {
			swProv.shutdown();
		}
	}

	@Override
	protected void onPostExecute (final Result<SuccessWhaleColumns> result) {
		if (result.isSuccess()) {
			this.onColumns.onAnswer(result.getData());
			this.dialog.dismiss();
		}
		else {
			this.dialog.dismiss();
			LOG.e("Failed fetch SuccessWhale columns.", result.getE());
			DialogHelper.alert(this.context, result.getE());
		}
	}

}
