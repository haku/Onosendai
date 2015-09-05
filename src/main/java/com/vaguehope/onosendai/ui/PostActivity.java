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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.TextKeyListener;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.PopupMenu;
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
	public static final String ARG_ALSO_MENTIONS = "also_mentions";
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
	private String[] alsoMentions;
	private Long outboxUid;

	private DbClient bndDb;
	private HybridBitmapCache imageCache;
	private ExecutorService exec;
	private Prefs prefs;

	private AccountAdaptor accountAdaptor;
	private Spinner spnAccount;
	private ViewGroup llSubAccounts;
	private MultiAutoCompleteTextView txtBody;

	private final EnabledServiceRefs enabledPostToAccounts = new EnabledServiceRefs();
	private boolean askAccountOnActivate;
	private Uri attachment;
	private Uri tmpAttachment;

	@Override
	protected void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.post);

		Collection<Account> accounts;
		Config conf;
		try {
			this.prefs = new Prefs(getBaseContext());
			accounts = this.prefs.readAccounts();
			conf = this.prefs.asConfig();
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
			this.alsoMentions = this.intentExtras.getStringArray(ARG_ALSO_MENTIONS);

			final long outboxUidOrDefault = this.intentExtras.getLong(ARG_OUTBOX_UID, -1);
			if (outboxUidOrDefault >= 0) this.outboxUid = outboxUidOrDefault;
		}
		LOG.i("inReplyToUid=%d inReplyToSid=%s altReplyToSid=%s alsoMentions=%s outboxUid=%s",
				this.inReplyToUid, this.inReplyToSid, this.altReplyToSid, Arrays.toString(this.alsoMentions), this.outboxUid);

		setupAccounts(savedInstanceState, accounts, conf);
		setupAttachemnt(savedInstanceState);
		setupTextBody();
		wireMainButtons();
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

		this.spnAccount = (Spinner) findViewById(R.id.spnAccount);
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
		((Button) findViewById(R.id.btnMenu)).setOnClickListener(this.menuButtonClickListener);
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

	private void wireMainButtons () {
		((Button) findViewById(R.id.btnCancel)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (final View v) {
				finish();
			}
		});

		final Button btnPost = (Button) findViewById(R.id.btnPost);
		btnPost.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (final View v) {
				askPost();
			}
		});
		if (this.outboxUid != null) btnPost.setText(R.string.post_btn_update_post);
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
		Tweet tweet = null;
		if (this.inReplyToSid != null && this.inReplyToUid > 0L) {
			final View view = findViewById(R.id.tweetReplyToDetails);
			view.setVisibility(View.VISIBLE);
			tweet = getDb().getTweetDetails(this.inReplyToUid);
			if (tweet != null) {
				LOG.i("inReplyTo:%s", tweet.toFullString());
				if (!this.enabledPostToAccounts.isServicesPreSpecified()) {
					final Meta serviceMeta = tweet.getFirstMetaOfType(MetaType.SERVICE);
					if (serviceMeta != null) setPostToAccountExclusive(ServiceRef.parseServiceMeta(serviceMeta));
				}

				((TextView) view.findViewById(R.id.tweetDetailBody)).setText(tweet.getBody());
				if (tweet.getAvatarUrl() != null) loadImage(new ImageLoadRequest(tweet.getAvatarUrl(), (ImageView) view.findViewById(R.id.tweetDetailAvatar)));
				((TextView) view.findViewById(R.id.tweetDetailName)).setText(tweet.getFullname());
				((TextView) view.findViewById(R.id.tweetDetailDate)).setText(DateHelper.formatDateTime(this, TimeUnit.SECONDS.toMillis(tweet.getTime())));
			}
		}
		initBody(tweet);
	}

	private void setPostToAccountExclusive (final ServiceRef svc) {
		if (svc == null) return;
		LOG.d("setPostToAccountExclusive(%s)", svc);
		this.enabledPostToAccounts.enableExclusiveAndSetPreSpecified(svc);
		PostToAccountLoaderTask.setAccountBtns(this.llSubAccounts, this.enabledPostToAccounts);
	}

	private void initBody (final Tweet tweet) {
		final String intialBody = this.intentExtras != null ? this.intentExtras.getString(ARG_BODY) : null;
		if (intialBody != null) {
			this.txtBody.setText(intialBody);
		}
		else {
			final StringBuilder s = new StringBuilder();
			if (tweet != null && tweet.getUsername() != null) s.append("@").append(StringHelper.firstLine(tweet.getUsername()));
			if (this.alsoMentions != null) {
				for (final String mention : this.alsoMentions) {
					s.append(" @").append(mention);
				}
			}
			if (s.length() > 0) {
				s.append(" ");
				this.txtBody.setText(s.toString());
			}
		}

		final int cursorPosition = this.intentExtras != null ? this.intentExtras.getInt(ARG_BODY_CURSOR_POSITION, -1) : -1;
		this.txtBody.setSelection(cursorPosition >= 0 ? cursorPosition : this.txtBody.getText().length());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected void askPost () {
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

		dlgBld.setPositiveButton(this.outboxUid == null ? R.string.post_confirm_post_btn_post : R.string.post_confirm_post_btn_update_post,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				submitPostToOutput(account, svcs);
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

	private String getReplyToSidToSubmit () {
		if (!StringHelper.isEmpty(this.altReplyToSid)) return this.altReplyToSid;
		return this.inReplyToSid;
	}

	protected void submitPostToOutput (final Account account, final Set<ServiceRef> svcs) {
		new SubmitToOutboxTask(this,
				new OutboxTweet(OutboxAction.POST, account, svcs, this.txtBody.getText().toString(), getReplyToSidToSubmit(), this.attachment),
				this.outboxUid).execute();
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

	private final OnClickListener menuButtonClickListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			showMenu(v);
		}
	};

	protected void showMenu (final View v) {
		final PopupMenu popupMenu = new PopupMenu(this, v);
		popupMenu.getMenuInflater().inflate(R.menu.postmenu, popupMenu.getMenu());
		popupMenu.setOnMenuItemClickListener(this.menuClickListener);
		if (this.attachment == null) {
			popupMenu.getMenu().findItem(R.id.mnuAttachmentGroup).setVisible(false);
		}
		else {
			popupMenu.getMenu().findItem(R.id.mnuAttach).setVisible(false);
		}
		popupMenu.show();
	}

	protected final PopupMenu.OnMenuItemClickListener menuClickListener = new PopupMenu.OnMenuItemClickListener() {
		@Override
		public boolean onMenuItemClick (final MenuItem item) {
			return menuItemClick(item);
		}
	};

	protected boolean menuItemClick (final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.mnuTextFilter:
				showTextFiltersDlg();
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

		private final Activity activity;
		private final OutboxTweet ot;
		private final Long outboxUid;
		private ProgressDialog dialog;

		public SubmitToOutboxTask (final Activity activity, final OutboxTweet ot, final Long outboxUid) {
			super(null, activity);
			this.activity = activity;
			this.ot = ot;
			this.outboxUid = outboxUid;
		}

		@Override
		protected LogWrapper getLog () {
			return LOG;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(getContext(), "Adding to Outbox", "...", true); //ES
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
				if (this.outboxUid == null) {
					db.addPostToOutput(otToAdd);
				}
				else {
					db.updateOutboxEntry(otToAdd.withUid(this.outboxUid).resetToPending());
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
				final String msg;
				if (this.outboxUid == null) {
					msg = "Posted via Outbox"; //ES
				}
				else {
					msg = "Updated Outbox item"; //ES
				}
				getContext().startService(new Intent(getContext(), SendOutboxService.class));
				Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
				this.activity.finish();
			}
			else {
				LOG.e("Failed to add to outbox.", result);
				DialogHelper.alert(getContext(), result);
			}
		}

	}

}
