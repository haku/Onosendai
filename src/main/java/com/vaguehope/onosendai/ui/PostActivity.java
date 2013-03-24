package com.vaguehope.onosendai.ui;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.images.HybridBitmapCache;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.images.ImageLoaderUtils;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.storage.DbClient;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.LogWrapper;

public class PostActivity extends Activity implements ImageLoader {

	public static final String ARG_COLUMN_ID = "column_id";
	public static final String ARG_IN_REPLY_TO = "in_reply_to";

	protected static final LogWrapper LOG = new LogWrapper("PA");

	private long inReplyTo;
	private int columnId;

	private DbClient bndDb;
	private HybridBitmapCache imageCache;

	private EditText txtBody;
	private TextView txtCharRemaining;

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

		final Bundle extras = getIntent().getExtras();
		this.columnId = extras.getInt(ARG_COLUMN_ID, -1);
		this.inReplyTo = extras.getLong(ARG_IN_REPLY_TO, -1);
		LOG.i("columnId=%d inReplyTo=%d", this.columnId, this.inReplyTo);

		this.imageCache = new HybridBitmapCache(this, C.MAX_MEMORY_IMAGE_CACHE);

		final Spinner spnAccount = (Spinner) findViewById(R.id.spnAccount);
		final AccountAdaptor accountAdaptor = new AccountAdaptor(this, conf);
		spnAccount.setAdapter(accountAdaptor);
		final Column column = conf.getColumnById(this.columnId);
		final Account account = conf.getAccount(column.accountId);
		spnAccount.setSelection(accountAdaptor.getAccountPosition(account));

		this.txtBody = (EditText) findViewById(R.id.txtBody);
		this.txtCharRemaining = (TextView) findViewById(R.id.txtCharRemaining);
		this.txtBody.addTextChangedListener(new TextCounterWatcher(this.txtCharRemaining, this.txtBody));

		((Button) findViewById(R.id.btnCancel)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (final View v) {
				finish();
			}
		});

		((Button) findViewById(R.id.btnPost)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (final View v) {
				Toast.makeText(PostActivity.this, "TODO: post", Toast.LENGTH_LONG).show();
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

	@Override
	public void onPause () {
		super.onPause();
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
		if (this.inReplyTo < 0) return;
		final View view = findViewById(R.id.tweetReplyToDetails);
		view.setVisibility(View.VISIBLE);
		final Tweet tweet = getDb().getTweetDetails(this.columnId, this.inReplyTo);
		if (tweet == null) return;
		((TextView) view.findViewById(R.id.tweetDetailBody)).setText(tweet.getBody());
		if (tweet.getAvatarUrl() != null) loadImage(new ImageLoadRequest(tweet.getAvatarUrl(), (ImageView) view.findViewById(R.id.tweetDetailAvatar)));
		((TextView) view.findViewById(R.id.tweetDetailName)).setText(tweet.getUsername());
		((TextView) view.findViewById(R.id.tweetDetailDate)).setText(DateFormat.getDateTimeInstance().format(new Date(tweet.getTime() * 1000L)));
		this.txtBody.setText("@" + tweet.getUsername() + " " + this.txtBody.getText());
		this.txtBody.setSelection(this.txtBody.getText().length());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static class AccountAdaptor extends BaseAdapter {

		private final LayoutInflater layoutInflater;
		private final List<Account> accounts;

		public AccountAdaptor (final Context context, final Config conf) {
			this.layoutInflater = LayoutInflater.from(context);
			this.accounts = new ArrayList<Account>(conf.getAccounts().values());
		}

		public int getAccountPosition (final Account account) {
			return this.accounts.indexOf(account);
		}

		@Override
		public int getCount () {
			return this.accounts.size();
		}

		@Override
		public Object getItem (final int position) {
			return this.accounts.get(position);
		}

		@Override
		public long getItemId (final int position) {
			return position;
		}

		@Override
		public View getView (final int position, final View convertView, final ViewGroup parent) {
			View view = convertView;
			RowView rowView;
			if (view == null) {
				view = this.layoutInflater.inflate(R.layout.accountlistrow, null);
				rowView = new RowView(
						(TextView) view.findViewById(R.id.txtMain)
						);
				view.setTag(rowView);
			}
			else {
				rowView = (RowView) view.getTag();
			}

			Account account = this.accounts.get(position);
			rowView.getMain().setText(String.format("%s (%s)", account.provider.toHumanString(), account.id));

			return view;
		}

		private static class RowView {

			private final TextView main;

			public RowView (final TextView main) {
				this.main = main;
			}

			public TextView getMain () {
				return this.main;
			}

		}

	}

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
