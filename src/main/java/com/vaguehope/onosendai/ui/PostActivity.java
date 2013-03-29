package com.vaguehope.onosendai.ui;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountAdaptor;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.images.HybridBitmapCache;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.images.ImageLoaderUtils;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.provider.PostTask;
import com.vaguehope.onosendai.provider.PostTask.PostRequest;
import com.vaguehope.onosendai.provider.successwhale.PostToAccount;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleException;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleProvider;
import com.vaguehope.onosendai.storage.DbClient;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.LogWrapper;

public class PostActivity extends Activity implements ImageLoader {

	public static final String ARG_COLUMN_ID = "column_id";
	public static final String ARG_IN_REPLY_TO_SID = "in_reply_to_sid";
	public static final String ARG_ALSO_MENTIONS = "also_mentions";
	public static final String ARG_BODY = "body"; // If present mentions will not be prepended to body.

	protected static final LogWrapper LOG = new LogWrapper("PA");

	private Bundle intentExtras;
	private int columnId;
	private String inReplyToSid;
	private String[] alsoMentions;

	private DbClient bndDb;
	private HybridBitmapCache imageCache;

	protected AccountAdaptor accountAdaptor;
	private Spinner spnAccount;
	private EditText txtBody;

	@Override
	protected void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.post);

		Config conf = null;
		try {
			conf = new Config();
		}
		catch (Exception e) {
			Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		this.intentExtras = getIntent().getExtras();
		this.columnId = this.intentExtras.getInt(ARG_COLUMN_ID, -1);
		this.inReplyToSid = this.intentExtras.getString(ARG_IN_REPLY_TO_SID);
		this.alsoMentions = this.intentExtras.getStringArray(ARG_ALSO_MENTIONS);
		LOG.i("columnId=%d inReplyTo=%s", this.columnId, this.inReplyToSid);

		this.imageCache = new HybridBitmapCache(getBaseContext(), C.MAX_MEMORY_IMAGE_CACHE);

		this.spnAccount = (Spinner) findViewById(R.id.spnAccount);
		this.accountAdaptor = new AccountAdaptor(getBaseContext(), conf);
		this.spnAccount.setAdapter(this.accountAdaptor);
		final Column column = conf.getColumnById(this.columnId);
		final Account account = conf.getAccount(column.getAccountId());
		this.spnAccount.setSelection(this.accountAdaptor.getAccountPosition(account));
		this.spnAccount.setOnItemSelectedListener(this.accountOnItemSelectedListener);

		this.txtBody = (EditText) findViewById(R.id.txtBody);
		final TextView txtCharRemaining = (TextView) findViewById(R.id.txtCharRemaining);
		this.txtBody.addTextChangedListener(new TextCounterWatcher(txtCharRemaining, this.txtBody));

		((Button) findViewById(R.id.btnCancel)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (final View v) {
				finish();
			}
		});

		((Button) findViewById(R.id.btnPost)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (final View v) {
				submitPost();
			}
		});
	}

	@Override
	public void onDestroy () {
		if (this.imageCache != null) this.imageCache.clean();
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
		ImageLoaderUtils.loadImage(this.imageCache, req);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void resumeDb () {
		LOG.d("Binding DB service...");
		if (this.bndDb == null) {
			this.bndDb = new DbClient(this, LOG.getPrefix(), new Runnable() {
				@Override
				public void run () {
					LOG.d("DB service bound.");
					showInReplyToTweetDetails();
				}
			});
		}
	}

	protected DbInterface getDb () {
		final DbClient d = this.bndDb;
		if (d == null) return null;
		return d.getDb();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected void showInReplyToTweetDetails () {
		Tweet tweet = null;
		if (this.inReplyToSid != null) {
			final View view = findViewById(R.id.tweetReplyToDetails);
			view.setVisibility(View.VISIBLE);
			tweet = getDb().getTweetDetails(this.columnId, this.inReplyToSid);
			if (tweet != null) {
				((TextView) view.findViewById(R.id.tweetDetailBody)).setText(tweet.getBody());
				if (tweet.getAvatarUrl() != null) loadImage(new ImageLoadRequest(tweet.getAvatarUrl(), (ImageView) view.findViewById(R.id.tweetDetailAvatar)));
				((TextView) view.findViewById(R.id.tweetDetailName)).setText(tweet.getFullname());
				((TextView) view.findViewById(R.id.tweetDetailDate)).setText(DateFormat.getDateTimeInstance().format(new Date(TimeUnit.SECONDS.toMillis(tweet.getTime()))));
			}
		}
		initBody(tweet);
		this.txtBody.setSelection(this.txtBody.getText().length());
	}

	private void initBody (final Tweet tweet) {
		final String intialBody = this.intentExtras.getString(ARG_BODY);
		if (intialBody != null) {
			this.txtBody.setText(intialBody);
		}
		else {
			StringBuilder s = new StringBuilder();
			if (tweet != null) s.append("@").append(tweet.getUsername());
			if (this.alsoMentions != null) {
				for (String mention : this.alsoMentions) {
					s.append(" @").append(mention);
				}
			}
			if (s.length() > 0) {
				s.append(" ");
				this.txtBody.setText(s.toString());
			}
		}

	}

	protected void submitPost () {
		final Account account = this.accountAdaptor.getAccount(this.spnAccount.getSelectedItemPosition());
		final String body = this.txtBody.getText().toString();
		final Intent recoveryIntent = new Intent(getBaseContext(), PostActivity.class)
				.putExtras(this.intentExtras)
				.putExtra(ARG_BODY, body);
		new PostTask(getBaseContext(), new PostRequest(account, body, this.inReplyToSid, recoveryIntent)).execute();
		finish();
	}

	private final OnItemSelectedListener accountOnItemSelectedListener = new OnItemSelectedListener() {
		@Override
		public void onItemSelected (final AdapterView<?> parent, final View view, final int position, final long id) {
			accountSelected(PostActivity.this.accountAdaptor.getAccount(position));
		}

		@Override
		public void onNothingSelected (final AdapterView<?> arg0) {/**/}
	};

	protected void accountSelected (final Account account) {
		ViewGroup llSubAccounts = (ViewGroup) findViewById(R.id.llSubAccounts);
		switch (account.getProvider()) {
			case TWITTER:
				llSubAccounts.setVisibility(View.GONE);
				break;
			case SUCCESSWHALE:
				new SwPostToAccountLoaderTask(llSubAccounts).execute(account);
				break;
			default:
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	TODO move these nested classes somewhere more sensible.

	private static class SwPostToAccountLoaderTask extends AsyncTask<Account, Void, AccountLoaderResult> {

		private final ViewGroup llSubAccounts;

		public SwPostToAccountLoaderTask (final ViewGroup llSubAccounts) {
			this.llSubAccounts = llSubAccounts;
		}

		@Override
		protected void onPreExecute () {
			this.llSubAccounts.removeAllViews();
			final ProgressBar progressBar = new ProgressBar(this.llSubAccounts.getContext());
			progressBar.setIndeterminate(true);
			this.llSubAccounts.addView(progressBar);
			this.llSubAccounts.setVisibility(View.VISIBLE);
		}

		@Override
		protected AccountLoaderResult doInBackground (final Account... params) {
			if (params.length != 1) throw new IllegalArgumentException("Only one account per task.");
			final Account account = params[0];

			SuccessWhaleProvider swProv = new SuccessWhaleProvider();
			try {
				return new AccountLoaderResult(swProv.getPostToAccounts(account));
			}
			catch (SuccessWhaleException e) {
				return new AccountLoaderResult(e);
			}
			finally {
				swProv.shutdown();
			}
		}

		@Override
		protected void onPostExecute (final AccountLoaderResult result) {
			this.llSubAccounts.removeAllViews();
			if (result.isSuccess()) {
				for (PostToAccount pta : result.getAccounts()) {
					final View view = View.inflate(this.llSubAccounts.getContext(), R.layout.subaccountitem, null);
					final ToggleButton btnEnableAccount = (ToggleButton) view.findViewById(R.id.btnEnableAccount);
					final String displayName = pta.getDisplayName();
					btnEnableAccount.setTextOn(displayName);
					btnEnableAccount.setTextOff(displayName);
					btnEnableAccount.setChecked(pta.isEnabled());
					this.llSubAccounts.addView(view);
				}
			}
			else {
				Toast.makeText(this.llSubAccounts.getContext(),
						"Failed to fetch sub accounts: " + result.getE().toString(),
						Toast.LENGTH_LONG).show();
			}
		}

	}

	private static class AccountLoaderResult {

		private final boolean success;
		private final List<PostToAccount> accounts;
		private final Exception e;

		public AccountLoaderResult (final List<PostToAccount> accounts) {
			if (accounts == null) throw new IllegalArgumentException("Missing arg: accounts.");
			this.success = true;
			this.accounts = accounts;
			this.e = null;
		}

		public AccountLoaderResult (final Exception e) {
			if (e == null) throw new IllegalArgumentException("Missing arg: e.");
			this.success = false;
			this.accounts = null;
			this.e = e;
		}

		public boolean isSuccess () {
			return this.success;
		}

		public List<PostToAccount> getAccounts () {
			return this.accounts;
		}

		public Exception getE () {
			return this.e;
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static class TextCounterWatcher implements TextWatcher {

		private final TextView txtView;
		private final EditText editText;

		public TextCounterWatcher (final TextView txtView, final EditText editText) {
			this.txtView = txtView;
			this.editText = editText;
		}

		@Override
		public void afterTextChanged (final Editable s) {
			this.txtView.setText(String.valueOf(this.editText.getText().length()));
		}

		@Override
		public void onTextChanged (final CharSequence s, final int start, final int before, final int count) {/**/}

		@Override
		public void beforeTextChanged (final CharSequence s, final int start, final int count, final int after) {/**/}
	}

}
