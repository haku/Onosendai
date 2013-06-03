package com.vaguehope.onosendai.payload;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;

import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.Result;

public class ReplyLoaderTask extends AsyncTask<Tweet, Void, Result<List<Payload>>> {

	private final Context context;
	private final DbInterface db;
	private final PayloadListAdapter payloadListAdaptor;

	public ReplyLoaderTask (final Context context, final DbInterface db, final PayloadListAdapter payloadListAdaptor) {
		this.context = context;
		this.db = db;
		this.payloadListAdaptor = payloadListAdaptor;
	}

	@Override
	protected Result<List<Payload>> doInBackground (final Tweet... params) {
		try {
			if (params.length != 1) throw new IllegalArgumentException("Only one param per task.");
			final Tweet tweet = params[0];

			final List<Tweet> replies = this.db.findTweetsWithMeta(MetaType.INREPLYTO, tweet.getSid(), 20);
			final List<Payload> replyPayloads = new ArrayList<Payload>();
			for (Tweet reply : replies) {
				replyPayloads.add(new InReplyToPayload(tweet, reply));
			}
			return new Result<List<Payload>>(replyPayloads);
		}
		catch (Exception e) { // NOSONAR want to report errors to UI.
			return new Result<List<Payload>>(e);
		}
	}

	@Override
	protected void onPostExecute (final Result<List<Payload>> result) {
		if (result.isSuccess()) {
			this.payloadListAdaptor.addItemsTop(result.getData());
		}
		else {
			DialogHelper.alert(this.context, result.getE());
		}
	}

}
