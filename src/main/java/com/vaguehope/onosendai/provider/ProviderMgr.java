package com.vaguehope.onosendai.provider;

import android.os.AsyncTask;

import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleProvider;
import com.vaguehope.onosendai.provider.twitter.TwitterProvider;
import com.vaguehope.onosendai.storage.KvStore;

public class ProviderMgr {

	private final TwitterProvider twitterProvider;
	private final SuccessWhaleProvider successWhaleProvider;

	public ProviderMgr (final KvStore kvStore) {
		this.twitterProvider = new TwitterProvider();
		this.successWhaleProvider = new SuccessWhaleProvider(kvStore);
	}

	public TwitterProvider getTwitterProvider () {
		return this.twitterProvider;
	}

	public SuccessWhaleProvider getSuccessWhaleProvider () {
		return this.successWhaleProvider;
	}

	public void shutdown () {
		try {
			this.twitterProvider.shutdown();
		}
		finally {
			new SwShutdowner().execute(this.successWhaleProvider);
		}
	}

	/**
	 * Calling shutdown on UI thread triggers android.os.NetworkOnMainThreadException.  lolz.
	 */
	private static class SwShutdowner extends AsyncTask<SuccessWhaleProvider, Void, Void> {

		public SwShutdowner () {}

		@Override
		protected Void doInBackground (final SuccessWhaleProvider... sws) {
			if (sws == null) return null;
			for (SuccessWhaleProvider sw : sws) {
				sw.shutdown();
			}
			return null;
		}

	}

}
