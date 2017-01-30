package com.vaguehope.onosendai.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.method.TextKeyListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountAdaptor;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.images.HybridBitmapCache;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.images.ImageLoaderUtils;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.OutboxTweet;
import com.vaguehope.onosendai.model.OutboxTweet.OutboxAction;
import com.vaguehope.onosendai.model.OutboxTweet.OutboxTweetStatus;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.provider.EnabledServiceRefs;
import com.vaguehope.onosendai.provider.SendOutboxService;
import com.vaguehope.onosendai.provider.ServiceRef;
import com.vaguehope.onosendai.storage.AttachmentStorage;
import com.vaguehope.onosendai.storage.DbBindingAsyncTask;
import com.vaguehope.onosendai.storage.DbClient;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.storage.DbProvider;
import com.vaguehope.onosendai.storage.UsernameSearchAdapter;
import com.vaguehope.onosendai.util.DateHelper;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;
import com.vaguehope.onosendai.util.ImageMetadata;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.Result;
import com.vaguehope.onosendai.util.StringHelper;
import com.vaguehope.onosendai.util.exec.ExecUtils;
import com.vaguehope.onosendai.widget.adaptor.PopupPositioniner;
import com.vaguehope.onosendai.widget.adaptor.TextCounterWatcher;
import com.vaguehope.onosendai.widget.adaptor.UsernameTokenizer;

public class PostActivity extends Activity implements ImageLoader, DbProvider {

	public static final String ARG_ACCOUNT_ID = "account_id";
	public static final String ARG_IN_REPLY_TO_SID = "in_reply_to_sid";
	public static final String ARG_IN_REPLY_TO_UID = "in_reply_to_uid";
	/**
	 * See MetaType.REPLYTO
	 */
	public static final String ARG_ALT_REPLY_TO_SID = "reply_to_sid";
	public static final String ARG_MENTIONS = "mentions";
	public static final String ARG_BODY = "body"; // If present mentions will not be prepended to body.
	public static final String ARG_BODY_CURSOR_POSITION = "cursor_position";
	public static final String ARG_SVCS = "svcs";
	public static final String ARG_ATTACHMENT = "post_attachment_uri";
	public static final String ARG_OUTBOX_UID = "outbox_uid";
	private static final String ARG_TMP_ATTACHMENT = "post_tmp_attachment_uri";

	protected static final LogWrapper LOG = new LogWrapper("PA");

	private static final int PROMPT_RESIZE_MIN_SIZE = 512 * 1024;

	private Bundle intentExtras;
	private long inReplyToUid;
	private String inReplyToSid;
	private String altReplyToSid;
	private String[] mentions;
	private Long outboxUid;

	private DbClient bndDb;
	private HybridBitmapCache imageCache;
	private ExecutorService exec;
	private Prefs prefs;
	private Config conf;

	private AccountAdaptor accountAdaptor;
	private Spinner spnAccount;
	private ViewGroup llSubAccounts;
	private MultiAutoCompleteTextView txtBody;

	private final EnabledServiceRefs enabledPostToAccounts = new EnabledServiceRefs();
	private boolean askAccountOnActivate;
	private String txtBodyInitialText;
	private Uri attachment;
	private Uri tmpAttachment;

	@Override
	protected void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.post);

		Collection<Account> accounts;
		try {
			this.prefs = new Prefs(getBaseContext());
			accounts = this.prefs.readAccounts();
			this.conf = this.prefs.asConfig();
		}
		catch (final Exception e) { // No point continuing if any exception.
			DialogHelper.alertAndClose(this, e);
			return;
		}

		this.imageCache = new HybridBitmapCache(getBaseContext(), C.MAX_MEMORY_IMAGE_CACHE);
		this.exec = ExecUtils.newBoundedCachedThreadPool(C.NET_MAX_THREADS, LOG);

		this.intentExtras = getIntent().getExtras();
		if (this.intentExtras != null) {
			this.inReplyToUid = this.intentExtras.getLong(ARG_IN_REPLY_TO_UID);
			this.inReplyToSid = this.intentExtras.getString(ARG_IN_REPLY_TO_SID);
			this.altReplyToSid = this.intentExtras.getString(ARG_ALT_REPLY_TO_SID);
			this.mentions = this.intentExtras.getStringArray(ARG_MENTIONS);

			final long outboxUidOrDefault = this.intentExtras.getLong(ARG_OUTBOX_UID, -1);
			if (outboxUidOrDefault >= 0) this.outboxUid = outboxUidOrDefault;
		}
		LOG.i("inReplyToUid=%d inReplyToSid=%s altReplyToSid=%s mentions=%s outboxUid=%s",
				this.inReplyToUid, this.inReplyToSid, this.altReplyToSid, Arrays.toString(this.mentions), this.outboxUid);

		final ActionBar ab = getActionBar();
		ab.setDisplayHomeAsUpEnabled(true);
		ab.setDisplayShowTitleEnabled(false);
		ab.setDisplayShowCustomEnabled(true);

		this.spnAccount = new Spinner(ab.getThemedContext());
		this.spnAccount.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
		this.spnAccount.setPadding(0, 0, 0, 0);
		ab.setCustomView(this.spnAccount);

		setupAccounts(savedInstanceState, accounts, this.conf);

		setupAttachemnt(savedInstanceState);
		setupTextBody();
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
		if (this.askAccountOnActivate) {
			askAccountAndShrinkPicture();
			this.askAccountOnActivate = false;
		}
	}

	@Override
	protected void onSaveInstanceState (final Bundle outState) {
		outState.putString(ARG_ACCOUNT_ID, getSelectedAccount().getId());
		this.enabledPostToAccounts.addToBundle(outState);
		outState.putParcelable(ARG_ATTACHMENT, this.attachment);
		outState.putParcelable(ARG_TMP_ATTACHMENT, this.tmpAttachment);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed () {
		if (isTxtBodyUnedited()) {
			finish();
			return;
		}

		DialogHelper.askYesNo(this, "Save draft?", "Save", "Discard", new Runnable() { //ES
			@Override
			public void run () {
				saveDraft();
			}
		}, new Runnable() {
			@Override
			public void run () {
				finish();
			}
		});
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
					setupAutoComplete();
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

	private void setupAccounts (final Bundle savedInstanceState, final Collection<Account> accounts, final Config conf) {
		String accountId = null;
		Account account = null;

		if (savedInstanceState != null) accountId = savedInstanceState.getString(ARG_ACCOUNT_ID);
		if (accountId == null && this.intentExtras != null) accountId = this.intentExtras.getString(ARG_ACCOUNT_ID);
		if (accountId != null) {
			account = conf.getAccount(accountId);
			final List<String> svcs = this.intentExtras != null ? this.intentExtras.getStringArrayList(ARG_SVCS) : null;
			LOG.i("accountId=%s svcs=%s", account.getId(), svcs);

			this.enabledPostToAccounts.setAccount(account);
			this.enabledPostToAccounts.fromBundle(savedInstanceState);
			if (svcs != null && !this.enabledPostToAccounts.isServicesPreSpecified()) {
				for (final String svc : svcs) {
					this.enabledPostToAccounts.enable(ServiceRef.parseServiceMeta(svc));
				}
				this.enabledPostToAccounts.setServicesPreSpecified(true);
			}
		}
		else {
			this.askAccountOnActivate = true;
		}

		this.accountAdaptor = new AccountAdaptor(getBaseContext(), accounts);
		this.spnAccount.setAdapter(this.accountAdaptor);
		setSelectedAccount(account);
		this.spnAccount.setOnItemSelectedListener(this.accountOnItemSelectedListener);

		this.llSubAccounts = (ViewGroup) findViewById(R.id.llSubAccounts);
	}

	private void setupAttachemnt (final Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			this.attachment = savedInstanceState.getParcelable(ARG_ATTACHMENT);
			this.tmpAttachment = savedInstanceState.getParcelable(ARG_TMP_ATTACHMENT);
		}
		if (this.attachment == null && this.intentExtras != null) this.attachment = this.intentExtras.getParcelable(ARG_ATTACHMENT);
		if (this.attachment == null && Intent.ACTION_SEND.equals(getIntent().getAction())
				&& getIntent().getType() != null
				&& getIntent().getType().startsWith("image/")) {
			final Uri intentUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
			if (ImageMetadata.isUnderstoodResource(intentUri)) {
				this.attachment = intentUri;
			}
			else {
				DialogHelper.alert(this, "Unknown resource:\n" + intentUri);
			}
		}
		redrawAttachment();
	}

	private void setupTextBody () {
		this.txtBody = (MultiAutoCompleteTextView) findViewById(R.id.txtBody);
		final TextView txtCharRemaining = (TextView) findViewById(R.id.txtCharRemaining);
		this.txtBody.addTextChangedListener(new TextCounterWatcher(txtCharRemaining, this.txtBody));

		if (Intent.ACTION_SEND.equals(getIntent().getAction()) && "text/plain".equals(getIntent().getType())) {
			final String intentText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
			if (intentText != null) {
				this.txtBody.setText(intentText);
				this.txtBody.setSelection(this.txtBody.getText().length());
			}
		}
	}

	public void setupAutoComplete () {
		if (this.txtBody.getAdapter() != null) return;
		this.txtBody.setThreshold(1);
		this.txtBody.setTokenizer(new UsernameTokenizer());
		this.txtBody.setAdapter(new UsernameSearchAdapter(this));
		this.txtBody.addTextChangedListener(new PopupPositioniner(this.txtBody));
		this.txtBody.setKeyListener(TextKeyListener.getInstance(true, TextKeyListener.Capitalize.SENTENCES));
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected AccountAdaptor getAccountAdaptor () {
		return this.accountAdaptor;
	}

	private Account getSelectedAccount () {
		return this.accountAdaptor.getAccount(this.spnAccount.getSelectedItemPosition());
	}

	protected void setSelectedAccount (final Account account) {
		if (account == null) return;
		this.spnAccount.setSelection(this.accountAdaptor.getAccountPosition(account));
	}

	protected void askAccountAndShrinkPicture () {
		final List<Account> items = readAccountsOrAlert();
		if (items == null) return;
		DialogHelper.askItem(this, getString(R.string.post_account_dlg_title), items, new Listener<Account>() {
			@Override
			public void onAnswer (final Account account) {
				setSelectedAccount(account);
				checkAndAskShrinkPicture();
			}
		});
	}

	private List<Account> readAccountsOrAlert () {
		try {
			return new ArrayList<Account>(this.prefs.readAccounts());
		}
		catch (final JSONException e) {
			DialogHelper.alert(this, "Failed to read accounts.", e); //ES
			return null;
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected void showInReplyToTweetDetails () {
		if (this.inReplyToSid != null) {
			final Tweet tweet;
			if (this.inReplyToUid > 0L) {
				tweet = getDb().getTweetDetails(this.inReplyToUid);
			}
			else if (OutboxTweet.isTempSid(this.inReplyToSid)) {
				final OutboxTweet ot = getDb().getOutboxEntry(OutboxTweet.uidFromTempSid(this.inReplyToSid));
				tweet = ot != null ? ot.toTweet(this.conf) : null;
			}
			else {
				tweet = getDb().getTweetDetails(this.inReplyToSid);
			}

			if (tweet != null) {
				LOG.i("inReplyTo:%s", tweet.toFullString());
				if (!this.enabledPostToAccounts.isServicesPreSpecified()) {
					final Meta serviceMeta = tweet.getFirstMetaOfType(MetaType.SERVICE);
					if (serviceMeta != null) setPostToAccountExclusive(ServiceRef.parseServiceMeta(serviceMeta));
				}

				final View view = findViewById(R.id.tweetReplyToDetails);
				view.setVisibility(View.VISIBLE);

				((TextView) view.findViewById(R.id.tweetDetailBody)).setText(tweet.getBody());
				((TextView) view.findViewById(R.id.tweetDetailName)).setText(tweet.getFullname());

				final ImageView tweetAvatar = (ImageView) view.findViewById(R.id.tweetDetailAvatar);
				if (tweet.getAvatarUrl() != null) {
					loadImage(new ImageLoadRequest(tweet.getAvatarUrl(), tweetAvatar));
				}
				else {
					tweetAvatar.setVisibility(View.GONE);
				}

				final TextView tweetDate = (TextView) view.findViewById(R.id.tweetDetailDate);
				if (tweet.getTime() > 0L) {
					tweetDate.setText(DateHelper.formatDateTime(this, TimeUnit.SECONDS.toMillis(tweet.getTime())));
				}
				else {
					tweetDate.setVisibility(View.GONE);
				}
			}
		}
		initBody();
	}

	private void setPostToAccountExclusive (final ServiceRef svc) {
		if (svc == null) return;
		LOG.d("setPostToAccountExclusive(%s)", svc);
		this.enabledPostToAccounts.enableExclusiveAndSetPreSpecified(svc);
		PostToAccountLoaderTask.setAccountBtns(this.llSubAccounts, this.enabledPostToAccounts);
	}

	private void initBody () {
		final String intentBody = this.intentExtras != null ? this.intentExtras.getString(ARG_BODY) : null;
		final String initialBody;
		if (intentBody != null) {
			initialBody = intentBody;
		}
		else {
			final StringBuilder s = new StringBuilder();

			if (this.mentions != null) {
				for (final String mention : this.mentions) {
					if (s.length() > 0) s.append(" ");
					s.append("@").append(mention);
				}
			}

			if (s.length() > 0) {
				s.append(" ");
				initialBody = s.toString();
			}
			else {
				initialBody = "";
			}
		}
		this.txtBody.setText(initialBody);
		this.txtBodyInitialText = initialBody;

		final int cursorPosition = this.intentExtras != null ? this.intentExtras.getInt(ARG_BODY_CURSOR_POSITION, -1) : -1;
		this.txtBody.setSelection(cursorPosition >= 0 ? cursorPosition : this.txtBody.getText().length());
	}

	private boolean isTxtBodyUnedited () {
		if (this.txtBodyInitialText == null) return false;
		return this.txtBodyInitialText.length() == this.txtBody.getText().length()
				&& this.txtBodyInitialText.equals(this.txtBody.getText().toString());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected OutboxTweet makeOutboxTweet (final Account account, final Set<ServiceRef> svcs) {
		final String replyToSidToSubmit;
		if (!StringHelper.isEmpty(this.altReplyToSid)) {
			replyToSidToSubmit = this.altReplyToSid;
		}
		else {
			replyToSidToSubmit = this.inReplyToSid;
		}

		final OutboxTweet ot = new OutboxTweet(OutboxAction.POST, account, svcs, this.txtBody.getText().toString(), replyToSidToSubmit, this.attachment);
		if (this.outboxUid != null) {
			return ot.withUid(this.outboxUid);
		}
		else {
			return ot;
		}
	}

	protected void saveDraft () {
		final Account account = getSelectedAccount();
		final Set<ServiceRef> svcs = this.enabledPostToAccounts.copyOfServices();
		final OutboxTweet ot = makeOutboxTweet(account, svcs).setPaused();
		new SubmitToOutboxTask(this, ot, new Listener<Long>() {
			@Override
			public void onAnswer (final Long newId) {
				finish();
			}
		}).execute();
	}

	protected void askPost (final boolean andContinue) {
		final Account account = getSelectedAccount();
		final Set<ServiceRef> svcs = this.enabledPostToAccounts.copyOfServices();
		final AlertDialog.Builder dlgBld = new AlertDialog.Builder(this);

		String msg;
		switch (account.getProvider()) {
			case SUCCESSWHALE:
			case BUFFER:
				msg = getString(R.string.post_confirm_post_subaccounts, account.getUiTitle(), ServiceRef.humanList(svcs, "\n"));
				break;
			default:
				msg = getString(R.string.post_confirm_post, account.getUiTitle());
		}
		dlgBld.setMessage(msg);

		final int msgId;
		if (andContinue) {
			msgId = this.outboxUid == null
					? R.string.post_confirm_post_btn_post_and_continue
					: R.string.post_confirm_post_btn_update_post_and_continue;
		}
		else {
			msgId = this.outboxUid == null
					? R.string.post_confirm_post_btn_post
					: R.string.post_confirm_post_btn_update_post;
		}

		dlgBld.setPositiveButton(msgId,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				final OutboxTweet ot = makeOutboxTweet(account, svcs).setPending();
				submitPost(ot, andContinue);
			}
		});

		dlgBld.setNegativeButton(R.string.post_confirm_post_btn_cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int whichButton) {
				dialog.cancel();
			}
		});

		dlgBld.show();
	}

	protected void submitPost (final OutboxTweet ot, final boolean andContinue) {
		new SubmitToOutboxTask(this, ot, new Listener<Long>() {
			@Override
			public void onAnswer (final Long newId) {
				if (andContinue) {
					startActivity(new Intent(PostActivity.this, PostActivity.class)
							.putExtra(PostActivity.ARG_ACCOUNT_ID, ot.getAccountId())
							.putStringArrayListExtra(PostActivity.ARG_SVCS, new ArrayList<String>(ot.getSvcMetasList()))
							.putExtra(PostActivity.ARG_IN_REPLY_TO_SID,
									(ot.getUid() == null ? ot.withUid(newId) : ot).getTempSid()));
				}
				finish();
			}
		}).execute();
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
			case BUFFER:
				this.enabledPostToAccounts.setAccount(account);
				new PostToAccountLoaderTask(getApplicationContext(), this.llSubAccounts, this.enabledPostToAccounts).executeOnExecutor(this.exec, account);
				break;
			default:
				this.llSubAccounts.setVisibility(View.GONE);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public boolean onCreateOptionsMenu (final Menu menu) {
		getMenuInflater().inflate(R.menu.postmenu, menu);

		if (this.outboxUid != null) {
			final MenuItem mnuPost = menu.findItem(R.id.mnuPost);
			mnuPost.setTitle(R.string.post_btn_update_post);
		}

		new Handler().post(new Runnable() {
			@Override
			public void run () {
				extraMenuSetup();
			}
		});

		return true;
	}

	protected void extraMenuSetup () {
		final View mnuPost = findViewById(R.id.mnuPost);
		mnuPost.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick (final View v) {
				askPost(true);
				return true;
			}
		});
	}

	@Override
	public boolean onPrepareOptionsMenu (final Menu menu) {
		if (this.attachment == null) {
			menu.findItem(R.id.mnuAttach).setVisible(true);
			menu.findItem(R.id.mnuAttachmentGroup).setVisible(false);
		}
		else {
			menu.findItem(R.id.mnuAttach).setVisible(false);
			menu.findItem(R.id.mnuAttachmentGroup).setVisible(true);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected (final MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;
			case R.id.mnuPost:
				askPost(false);
				return true;
			case R.id.mnuTextFilter:
				showTextFiltersDlg();
				return true;
			case R.id.mnuSaveAsDraft:
				saveDraft();
				return true;

			case R.id.mnuAttach:
			case R.id.mnuAttachmentChange:
				askChoosePicture();
				return true;
			case R.id.mnuAttachmentShrink:
				showShrinkPictureDlg();
				return true;
			case R.id.mnuAttachmentRemove:
				setAndRedrawAttachment(null);
				return true;
			default:
				return false;
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final int SELECT_PICTURE = 105340; // NOSONAR Just a number.

	private ImageMetadata redrawAttachment () {
		final ImageMetadata metadata = new ImageMetadata(this, this.attachment);
		final TextView txtAttached = (TextView) findViewById(R.id.txtAttached);
		txtAttached.setText(getString(R.string.post_attachment_current, metadata.getUiTitle()));
		txtAttached.setVisibility(metadata.exists() ? View.VISIBLE : View.GONE);
		return metadata;
	}

	protected ImageMetadata setAndRedrawAttachment (final Uri att) {
		this.attachment = att;
		return redrawAttachment();
	}

	private void showTextFiltersDlg () {
		TextFiltersDialog.show(this, this.txtBody.getText().toString(), new Listener<String>() {
			@Override
			public void onAnswer (final String filteredText) {
				PostActivity.this.txtBody.setText(filteredText);
			}
		});
	}

	private void askChoosePicture () {
		this.tmpAttachment = Uri.fromFile(AttachmentStorage.getExternalTempFile(".jpg"));
		startActivityForResult(Intent.createChooser(
				new Intent(Intent.ACTION_GET_CONTENT)
						.setType("image/*")
						.addCategory(Intent.CATEGORY_OPENABLE),
				getString(R.string.post_attachment_ask_select))
				.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {
						new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
								.putExtra(MediaStore.EXTRA_OUTPUT, this.tmpAttachment) })
				, SELECT_PICTURE);
	}

	protected void showShrinkPictureDlg () {
		try {
			final PictureResizeDialog dlg = new PictureResizeDialog(this, this.attachment);
			final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);
			dlgBuilder.setTitle(dlg.getUiTitle());
			dlgBuilder.setView(dlg.getRootView());
			dlgBuilder.setPositiveButton(android.R.string.ok, new android.content.DialogInterface.OnClickListener() {
				@Override
				public void onClick (final DialogInterface dialog, final int which) {
					resizeAttachedPicture(dlg);
					dialog.dismiss();
				}
			});
			dlgBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() { //ES
				@Override
				public void onClick (final DialogInterface dialog, final int whichButton) {
					dialog.cancel();
				}
			});
			dlgBuilder.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel (final DialogInterface dialog) {
					dlg.recycle();
				}
			});
			dlgBuilder.create().show();
		}
		// XXX Cases of OutOfMemoryError have been reported, particularly on low end hardware.
		// Try not to upset the user too much by not dying completely if possible.
		catch (final Throwable t) {
			DialogHelper.alert(this, t);
		}
	}

	protected void resizeAttachedPicture (final PictureResizeDialog dlg) {
		new PictureResizeDialog.ResizeToTempFileTask(dlg, new Listener<Result<Uri>>() {
			@Override
			public void onAnswer (final Result<Uri> result) {
				dlg.recycle();
				if (result.isSuccess()) {
					setAndRedrawAttachment(result.getData());
				}
				else {
					LOG.e("Failed to resize image.", result.getE());
					DialogHelper.alert(PostActivity.this, result.getE());
				}
			}
		}).execute();
	}

	@Override
	protected void onActivityResult (final int requestCode, final int resultCode, final Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		switch (requestCode) {
			case SELECT_PICTURE:
				if (resultCode == RESULT_OK) onPictureChosen(intent);
				break;
			default:
		}
	}

	private void onPictureChosen (final Intent intent) {
		try {
			Uri uri;
			if (intent != null) {
				uri = intent.getData();
			}
			else if (this.tmpAttachment != null) {
				uri = Uri.fromFile(AttachmentStorage.moveToInternalStorage(this, new File(this.tmpAttachment.getPath())));
			}
			else {
				DialogHelper.alert(this, "Media is missing from response intent."); //ES
				return;
			}
			if (ImageMetadata.isUnderstoodResource(uri)) {
				this.attachment = uri;
				final ImageMetadata metadata = redrawAttachment();
				checkAndAskShrinkPicture(metadata);
			}
			else {
				DialogHelper.alert(this, "Unknown resource:\n" + uri); //ES
			}
		}
		catch (final IOException e) {
			DialogHelper.alert(this, e);
		}
	}

	protected void checkAndAskShrinkPicture () {
		checkAndAskShrinkPicture(new ImageMetadata(this, this.attachment));
	}

	private void checkAndAskShrinkPicture (final ImageMetadata metadata) {
		if (metadata.getSize() < PROMPT_RESIZE_MIN_SIZE) return;
		DialogHelper.askYesNo(this,
				getString(R.string.post_attachment_ask_shrink, IoHelper.readableFileSize(metadata.getSize())),
				getString(R.string.post_attachment_ask_shrink_btn_skrink),
				getString(R.string.post_attachment_ask_shrink_btn_full_size),
				new Runnable() {
					@Override
					public void run () {
						showShrinkPictureDlg();
					}
				});
	}

	private static class SubmitToOutboxTask extends DbBindingAsyncTask<Void, String, Exception> {

		private final OutboxTweet ot;
		private ProgressDialog dialog;
		private final Listener<Long> onSuccess;
		private volatile Long newId;

		public SubmitToOutboxTask (final Context context, final OutboxTweet ot, final Listener<Long> onSuccess) {
			super(null, context);
			this.ot = ot;
			this.onSuccess = onSuccess;
		}

		@Override
		protected LogWrapper getLog () {
			return LOG;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(getContext(), "Writing to Outbox", "...", true); //ES
		}

		@Override
		protected void onProgressUpdate (final String... msgs) {
			if (msgs == null || msgs.length < 1) return;
			this.dialog.setMessage(msgs[0]);
		}

		@Override
		protected Exception doInBackgroundWithDb (final DbInterface db, final Void... params) {
			try {
				final OutboxTweet otToAdd;
				if (this.ot.getAttachment() != null && ImageMetadata.isRemoteResource(this.ot.getAttachment())) {
					publishProgress("Caching attachment..."); //ES
					final ImageMetadata im = new ImageMetadata(getContext(), this.ot.getAttachment());
					final File internal = AttachmentStorage.getTempFile(getContext(), im.getName());
					final InputStream is = im.open();
					try {
						IoHelper.copy(is, internal);
					}
					finally {
						IoHelper.closeQuietly(is);
					}
					otToAdd = this.ot.withAttachment(Uri.fromFile(internal));
				}
				else {
					otToAdd = this.ot;
				}

				publishProgress("Writing to Outbox..."); //ES
				if (otToAdd.getUid() == null) {
					this.newId = db.addPostToOutput(otToAdd);
				}
				else {
					db.updateOutboxEntry(otToAdd);
				}

				return null;
			}
			catch (final IOException e) {
				return e;
			}
		}

		@Override
		protected void onPostExecute (final Exception result) {
			this.dialog.dismiss();
			if (result == null) {
				if (this.ot.getStatus() == OutboxTweetStatus.PENDING) {
					getContext().startService(new Intent(getContext(), SendOutboxService.class));

					final String msg;
					if (this.ot.getUid() == null) {
						msg = "Posted via Outbox"; //ES
					}
					else {
						msg = "Updated Outbox item"; //ES
					}
					Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
				}
				else if (this.ot.getStatus() == OutboxTweetStatus.PAUSED) {
					Toast.makeText(getContext(), "Draft Saved to Outbox", Toast.LENGTH_SHORT).show(); //ES
				}
				else {
					LOG.w("Unexpected OT status: " + this.ot.getStatus());
				}
				this.onSuccess.onAnswer(this.newId);
			}
			else {
				LOG.e("Failed to add to outbox.", result);
				DialogHelper.alert(getContext(), result);
			}
		}

	}

}
