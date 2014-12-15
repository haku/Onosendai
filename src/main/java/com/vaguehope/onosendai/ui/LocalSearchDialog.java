package com.vaguehope.onosendai.ui;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.model.TweetListAdapter;
import com.vaguehope.onosendai.storage.DbProvider;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.Result;

public class LocalSearchDialog {

	private static final LogWrapper LOG = new LogWrapper("LS");

	public interface OnTweetListener {
		void onTweet (int colId, Tweet tweet);
	}

	public static void show (final Context context, final Config conf, final DbProvider dbProvider, final ImageLoader imageLoader, final OnTweetListener onTweetListener) {
		final LocalSearchDialog lsDlg = new LocalSearchDialog(context, conf, dbProvider, imageLoader, onTweetListener);
		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(context);
		dlgBuilder.setView(lsDlg.getRootView());
		final AlertDialog dlg = dlgBuilder.create();
		lsDlg.setDialog(dlg);
		dlg.show();
	}

	private final Context context;
	private final Config conf;
	private final DbProvider dbProvider;
	private final View llParent;
	private final EditText txtSearch;
	private final TweetListAdapter tweetAdaptor;

	private Dialog dialog;

	private LocalSearchDialog (final Context context, final Config conf, final DbProvider dbProvider, final ImageLoader imageLoader, final OnTweetListener onTweetListener) {
		this.context = context;
		this.conf = conf;
		this.dbProvider = dbProvider;
		final LayoutInflater inflater = LayoutInflater.from(context);
		this.llParent = inflater.inflate(R.layout.localsearch, null);

		this.txtSearch = (EditText) this.llParent.findViewById(R.id.txtSearch);

		final ListView tweetList = (ListView) this.llParent.findViewById(R.id.tweetList);
		tweetList.setEmptyView(this.llParent.findViewById(R.id.empty));
		this.tweetAdaptor = new TweetListAdapter(context, false, imageLoader, tweetList);
		tweetList.setAdapter(this.tweetAdaptor);

		((Button) this.llParent.findViewById(R.id.btnSearch)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (final View v) {
				btnSearchClicked();
			}
		});

		tweetList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick (final AdapterView<?> parent, final View view, final int position, final long id) {
				final Tweet tweet = LocalSearchDialog.this.tweetAdaptor.getTweet(position);
				final int colId = tweet.getFirstMetaOfType(MetaType.COLUMN_ID).toInt(-1);
				onTweetListener.onTweet(colId, tweet);
				LocalSearchDialog.this.dialog.dismiss();
			}
		});
	}

	private void setDialog (final Dialog dialog) {
		this.dialog = dialog;
	}

	private View getRootView () {
		return this.llParent;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected void btnSearchClicked () {
		new SearchTweets(this, this.txtSearch.getText().toString()).execute();
	}

	private static class SearchTweets extends AsyncTask<Void, Void, Result<TweetList>> {

		private final LocalSearchDialog dlg;
		private final String searchTerm;

		private ProgressDialog dialog;

		public SearchTweets (final LocalSearchDialog dlg, final String searchTerm) {
			this.dlg = dlg;
			this.searchTerm = searchTerm;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(this.dlg.context, "Tweets", "Searching...", true);
		}

		@Override
		protected Result<TweetList> doInBackground (final Void... params) {
			try {
				final List<Tweet> tweets = this.dlg.dbProvider.getDb().searchTweets(this.searchTerm, this.dlg.conf.getColumns(), 50);
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
				this.dlg.tweetAdaptor.setInputData(result.getData());
			}
			else {
				LOG.e("Failed to run local search.", result.getE());
				DialogHelper.alert(this.dlg.context, result.getE());
			}
		}

	}

}
