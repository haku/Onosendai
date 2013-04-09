package com.vaguehope.onosendai.ui;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountAdaptor;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.images.HybridBitmapCache;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.images.ImageLoaderUtils;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.provider.PostTask;
import com.vaguehope.onosendai.provider.PostTask.PostRequest;
import com.vaguehope.onosendai.provider.successwhale.EnabledServiceRefs;
import com.vaguehope.onosendai.provider.successwhale.ServiceRef;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleProvider;
import com.vaguehope.onosendai.storage.DbClient;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.ExecUtils;
import com.vaguehope.onosendai.util.LogWrapper;

public class PostActivity extends Activity implements ImageLoader {

	public static final String ARG_ACCOUNT_ID = "account_id";
	public static final String ARG_IN_REPLY_TO_SID = "in_reply_to_sid";
	public static final String ARG_IN_REPLY_TO_UID = "in_reply_to_uid";
	public static final String ARG_ALSO_MENTIONS = "also_mentions";
	public static final String ARG_BODY = "body"; // If present mentions will not be prepended to body.
	public static final String ARG_SVCS = "svcs";

	protected static final LogWrapper LOG = new LogWrapper("PA");

	private Bundle intentExtras;
	private long inReplyToUid;
	private String inReplyToSid;
	private String[] alsoMentions;

	private DbClient bndDb;
	private HybridBitmapCache imageCache;
	private ExecutorService exec;

	private AccountAdaptor accountAdaptor;
	private Spinner spnAccount;
	private ViewGroup llSubAccounts;
	private EditText txtBody;
	private final EnabledServiceRefs enabledPostToAccounts = new EnabledServiceRefs();

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
		final String accountId = this.intentExtras.getString(ARG_ACCOUNT_ID);
		this.inReplyToUid = this.intentExtras.getLong(ARG_IN_REPLY_TO_UID);
		this.inReplyToSid = this.intentExtras.getString(ARG_IN_REPLY_TO_SID);
		this.alsoMentions = this.intentExtras.getStringArray(ARG_ALSO_MENTIONS);
		final List<String> svcs = this.intentExtras.getStringArrayList(ARG_SVCS);
		if (svcs != null) {
			for (String svc : svcs) {
				this.enabledPostToAccounts.enable(SuccessWhaleProvider.parseServiceMeta(svc));
			}
			this.enabledPostToAccounts.setServicesPreSpecified(true);
		}
		LOG.i("accountId=%s inReplyToUid=%d inReplyToSid=%s svcs=%s", accountId, this.inReplyToUid, this.inReplyToSid, svcs);

		this.imageCache = new HybridBitmapCache(getBaseContext(), C.MAX_MEMORY_IMAGE_CACHE);
		this.exec = ExecUtils.newBoundedCachedThreadPool(C.IMAGE_LOADER_MAX_THREADS);

		this.spnAccount = (Spinner) findViewById(R.id.spnAccount);
		this.accountAdaptor = new AccountAdaptor(getBaseContext(), conf);
		this.spnAccount.setAdapter(this.accountAdaptor);
		final Account account = conf.getAccount(accountId);
		this.spnAccount.setSelection(this.accountAdaptor.getAccountPosition(account));
		this.spnAccount.setOnItemSelectedListener(this.accountOnItemSelectedListener);

		this.llSubAccounts = (ViewGroup) findViewById(R.id.llSubAccounts);

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
				askPost();
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

	protected AccountAdaptor getAccountAdaptor () {
		return this.accountAdaptor;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected void showInReplyToTweetDetails () {
		Tweet tweet = null;
		if (this.inReplyToSid != null && this.inReplyToUid > 0L) {
			final View view = findViewById(R.id.tweetReplyToDetails);
			view.setVisibility(View.VISIBLE);
			tweet = getDb().getTweetDetails(this.inReplyToUid);
			if (tweet != null) {
				LOG.i("inReplyTo:%s", tweet.toFullString());
				if (!this.enabledPostToAccounts.isServicesPreSpecified()) {
					final Meta serviceMeta = tweet.getFirstMetaOfType(MetaType.SERVICE);
					if (serviceMeta != null) setPostToAccountExclusive(SuccessWhaleProvider.parseServiceMeta(serviceMeta));
				}

				((TextView) view.findViewById(R.id.tweetDetailBody)).setText(tweet.getBody());
				if (tweet.getAvatarUrl() != null) loadImage(new ImageLoadRequest(tweet.getAvatarUrl(), (ImageView) view.findViewById(R.id.tweetDetailAvatar)));
				((TextView) view.findViewById(R.id.tweetDetailName)).setText(tweet.getFullname());
				((TextView) view.findViewById(R.id.tweetDetailDate)).setText(DateFormat.getDateTimeInstance().format(new Date(TimeUnit.SECONDS.toMillis(tweet.getTime()))));
			}
		}
		initBody(tweet);
		this.txtBody.setSelection(this.txtBody.getText().length());
	}

	private void setPostToAccountExclusive (final ServiceRef svc) {
		if (svc == null) return;
		this.enabledPostToAccounts.enableExclusiveAndSetPreSpecified(svc);
		SwPostToAccountLoaderTask.setExclusiveSelectedAccountBtn(this.llSubAccounts, svc);
	}

	private void initBody (final Tweet tweet) {
		final String intialBody = this.intentExtras.getString(ARG_BODY);
		if (intialBody != null) {
			this.txtBody.setText(intialBody);
		}
		else {
			StringBuilder s = new StringBuilder();
			if (tweet != null && tweet.getUsername() != null) s.append("@").append(tweet.getUsername());
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

	protected void askPost () {
		final Account account = this.accountAdaptor.getAccount(this.spnAccount.getSelectedItemPosition());
		final Set<ServiceRef> svcs = this.enabledPostToAccounts.copyOfServices();
		final AlertDialog.Builder dlgBld = new AlertDialog.Builder(this);

		String msg;
		if (account.getProvider() == AccountProvider.SUCCESSWHALE) {
			msg = String.format("Post to these %s accounts?\n%s", account.toHumanString(), ServiceRef.humanList(svcs, "\n"));
		}
		else {
			msg = String.format("Post to %s?", account.toHumanString());
		}
		dlgBld.setMessage(msg);

		dlgBld.setPositiveButton("Post", new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				submitPost(account, svcs);
			}
		});

		dlgBld.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int whichButton) {
				dialog.cancel();
			}
		});

		dlgBld.show();
	}

	protected void submitPost (final Account account, final Set<ServiceRef> svcs) {
		final String body = this.txtBody.getText().toString();
		final Intent recoveryIntent = new Intent(getBaseContext(), PostActivity.class)
				.putExtra(ARG_ACCOUNT_ID, account.getId())
				.putExtra(ARG_IN_REPLY_TO_UID, this.inReplyToUid)
				.putExtra(ARG_IN_REPLY_TO_SID, this.inReplyToSid)
				.putExtra(ARG_BODY, body);

		final ArrayList<String> svcsLst = new ArrayList<String>();
		for (ServiceRef svc : svcs) {
			svcsLst.add(svc.toServiceMeta());
		}
		recoveryIntent.putStringArrayListExtra(ARG_SVCS, svcsLst);

		new PostTask(getApplicationContext(), new PostRequest(account, svcs, body, this.inReplyToSid, recoveryIntent)).execute();
		finish();
	}

	private final OnItemSelectedListener accountOnItemSelectedListener = new OnItemSelectedListener() {
		@Override
		public void onItemSelected (final AdapterView<?> parent, final View view, final int position, final long id) {
			accountSelected(getAccountAdaptor().getAccount(position));
		}

		@Override
		public void onNothingSelected (final AdapterView<?> arg0) {/**/}
	};

	protected void accountSelected (final Account account) {
		switch (account.getProvider()) {
			case SUCCESSWHALE:
				new SwPostToAccountLoaderTask(getApplicationContext(), this.llSubAccounts, this.enabledPostToAccounts).executeOnExecutor(this.exec, account);
				break;
			default:
				this.llSubAccounts.setVisibility(View.GONE);
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
