package com.vaguehope.onosendai.payload;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.Result;
import com.vaguehope.onosendai.util.exec.ExecutorEventListener;
import com.vaguehope.onosendai.util.exec.TrackingAsyncTask;

public class ReplyLoaderTask extends TrackingAsyncTask<Void, Void, Result<List<Payload>>> {

	private final Context context;
	private final DbInterface db;
	private final Tweet tweet;
	private final PayloadListAdapter payloadListAdaptor;

	public ReplyLoaderTask (final ExecutorEventListener eventListener, final Context context, final DbInterface db, final Tweet tweet, final PayloadListAdapter payloadListAdaptor) {
		super(eventListener);
		this.context = context;
		this.db = db;
		this.payloadListAdaptor = payloadListAdaptor;
		this.tweet = tweet;
	}

	@Override
	public String toString () {
		return "replyLoader:" + this.tweet.getSid();
	}

	@Override
	protected Result<List<Payload>> doInBackgroundWithTracking (final Void... unused) {
		try {
			final List<Tweet> replies = this.db.findTweetsWithMeta(MetaType.INREPLYTO, this.tweet.getSid(), 20);
			final List<Payload> replyPayloads = new ArrayList<Payload>();
			for (Tweet reply : replies) {
				replyPayloads.add(new InReplyToPayload(this.tweet, reply));
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
			if (this.payloadListAdaptor.isForTweet(this.tweet)) {
				this.payloadListAdaptor.addItemsTop(result.getData());
			}
		}
		else {
			DialogHelper.alert(this.context, result.getE());
		}
	}

}
