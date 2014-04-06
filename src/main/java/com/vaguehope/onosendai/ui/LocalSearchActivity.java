package com.vaguehope.onosendai.ui;

import java.util.List;
import java.util.concurrent.ExecutorService;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.images.HybridBitmapCache;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.images.ImageLoaderUtils;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.model.TweetListAdapter;
import com.vaguehope.onosendai.storage.DbClient;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.storage.DbProvider;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.Result;
import com.vaguehope.onosendai.util.exec.ExecUtils;

public class LocalSearchActivity extends Activity implements ImageLoader, DbProvider {

	private static final LogWrapper LOG = new LogWrapper("LS");

	private DbClient bndDb;
	private HybridBitmapCache imageCache;
	private ExecutorService exec;

	private EditText txtSearch;
	private TweetListAdapter tweetAdaptor;

	@Override
	protected void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.localsearch);

		this.imageCache = new HybridBitmapCache(getBaseContext(), C.MAX_MEMORY_IMAGE_CACHE);
		this.exec = ExecUtils.newBoundedCachedThreadPool(C.NET_MAX_THREADS, LOG);

		this.txtSearch = (EditText) findViewById(R.id.txtSearch);

		final ListView tweetList = (ListView) findViewById(R.id.tweetList);
		tweetList.setEmptyView(findViewById(R.id.empty));
		this.tweetAdaptor = new TweetListAdapter(this, false, this, tweetList);
		tweetList.setAdapter(this.tweetAdaptor);

		((Button) findViewById(R.id.btnSearch)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (final View v) {
				btnSearchClicked();
			}
		});
	}

	@Override
	public void onDestroy () {
		if (this.imageCache != null) this.imageCache.clean();
		if (this.exec != null) this.exec.shutdown();
		this.bndDb.dispose();
		super.onDestroy();
	}

	@Override
	public void onResume () {
		super.onResume();
		resumeDb();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void loadImage (final ImageLoadRequest req) {
		ImageLoaderUtils.loadImage(this.imageCache, req, this.exec);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void resumeDb () {
		if (this.bndDb == null) {
			LOG.d("Binding DB service...");
			this.bndDb = new DbClient(this, LOG.getPrefix(), new Runnable() {
				@Override
				public void run () {
					LOG.d("DB service bound.");
					// TODO
				}
			});
		}
	}

	@Override
	public DbInterface getDb () {
		final DbClient d = this.bndDb;
		if (d == null) return null;
		return d.getDb();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected void btnSearchClicked () {
		new SearchTweets(this, this.txtSearch.getText().toString()).execute();
	}

	private static class SearchTweets extends AsyncTask<Void, Void, Result<TweetList>> {

		private final LocalSearchActivity parent;
		private final String searchTerm;

		private ProgressDialog dialog;

		public SearchTweets (final LocalSearchActivity parent, final String searchTerm) {
			this.parent = parent;
			this.searchTerm = searchTerm;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(this.parent, "Tweets", "Searching...", true);
		}

		@Override
		protected Result<TweetList> doInBackground (final Void... params) {
			try {
				final List<Tweet> tweets = this.parent.getDb().searchTweets(this.searchTerm, 50);
				return new Result<TweetList>(new TweetList(tweets));
			}
			catch (final Exception e) { // NOSONAR needed to report errors.
				return new Result<TweetList>(e);
			}
		}

		@Override
		protected void onPostExecute (final Result<TweetList> result) {
			this.dialog.dismiss();
			if (result.isSuccess()) {
				this.parent.tweetAdaptor.setInputData(result.getData());
			}
			else {
				LOG.e("Failed to run local search.", result.getE());
				DialogHelper.alert(this.parent, result.getE());
			}
		}

	}

}
