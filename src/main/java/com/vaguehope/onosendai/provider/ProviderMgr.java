package com.vaguehope.onosendai.provider;

import android.os.AsyncTask;

import com.vaguehope.onosendai.provider.bufferapp.BufferAppProvider;
import com.vaguehope.onosendai.provider.hosaka.HosakaProvider;
import com.vaguehope.onosendai.provider.instapaper.InstapaperProvider;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleProvider;
import com.vaguehope.onosendai.provider.twitter.TwitterProvider;
import com.vaguehope.onosendai.storage.KvStore;

public class ProviderMgr {

	public interface ProviderMgrProvider {
		ProviderMgr getProviderMgr();
	}

	private final TwitterProvider twitterProvider;
	private final SuccessWhaleProvider successWhaleProvider;
	private final InstapaperProvider instapaperProvider;
	private final BufferAppProvider bufferAppProvider;
	private final HosakaProvider hosakaProvider;

	public ProviderMgr (final KvStore kvStore) {
		this.twitterProvider = new TwitterProvider();
		this.successWhaleProvider = new SuccessWhaleProvider(kvStore);
		this.instapaperProvider = new InstapaperProvider();
		this.bufferAppProvider = new BufferAppProvider();
		this.hosakaProvider = new HosakaProvider();
	}

	public TwitterProvider getTwitterProvider () {
		return this.twitterProvider;
	}

	public SuccessWhaleProvider getSuccessWhaleProvider () {
		return this.successWhaleProvider;
	}

	public InstapaperProvider getInstapaperProvider () {
		return this.instapaperProvider;
	}

	public BufferAppProvider getBufferAppProvider () {
		return this.bufferAppProvider;
	}

	public HosakaProvider getHosakaProvider () {
		return this.hosakaProvider;
	}

	/**
	 * TODO: tidy this up.
	 */
	public void shutdown () {
		try {
			this.twitterProvider.shutdown();
		}
		finally {
			try {
				new SwShutdowner().execute(this.successWhaleProvider);
			}
			finally {
				try {
					this.instapaperProvider.shutdown();
				}
				finally {
					try {
						this.bufferAppProvider.shutdown();
					}
					finally {
						this.hosakaProvider.shutdown();
					}
				}
			}
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
