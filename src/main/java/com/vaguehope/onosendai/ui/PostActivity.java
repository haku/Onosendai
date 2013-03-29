package com.vaguehope.onosendai.ui;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

	private Spinner spnAccount;
	private AccountAdaptor accountAdaptor;
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
