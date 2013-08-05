package com.vaguehope.onosendai.ui;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;

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
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.provider.EnabledServiceRefs;
import com.vaguehope.onosendai.provider.PostTask;
import com.vaguehope.onosendai.provider.PostTask.PostRequest;
import com.vaguehope.onosendai.provider.ServiceRef;
import com.vaguehope.onosendai.storage.AttachmentStorage;
import com.vaguehope.onosendai.storage.DbClient;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;
import com.vaguehope.onosendai.util.ExecUtils;
import com.vaguehope.onosendai.util.ImageMetadata;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.LogWrapper;

public class PostActivity extends Activity implements ImageLoader {

	public static final String ARG_ACCOUNT_ID = "account_id";
	public static final String ARG_IN_REPLY_TO_SID = "in_reply_to_sid";
	public static final String ARG_IN_REPLY_TO_UID = "in_reply_to_uid";
	public static final String ARG_ALSO_MENTIONS = "also_mentions";
	public static final String ARG_BODY = "body"; // If present mentions will not be prepended to body.
	public static final String ARG_SVCS = "svcs";
	public static final String ARG_ATTACHMENT = "post_attachment_uri";
	private static final String ARG_TMP_ATTACHMENT = "post_tmp_attachment_uri";

	protected static final LogWrapper LOG = new LogWrapper("PA");

	private static final int PROMPT_RESIZE_MIN_SIZE = 512 * 1024;

	private Bundle intentExtras;
	private long inReplyToUid;
	private String inReplyToSid;
	private String[] alsoMentions;

	private DbClient bndDb;
	private HybridBitmapCache imageCache;
	private ExecutorService exec;
	private Prefs prefs;

	private AccountAdaptor accountAdaptor;
	private Spinner spnAccount;
	private ViewGroup llSubAccounts;
	private EditText txtBody;

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
		catch (Exception e) { // No point continuing if any exception.
			DialogHelper.alertAndClose(this, e);
			return;
		}

		this.imageCache = new HybridBitmapCache(getBaseContext(), C.MAX_MEMORY_IMAGE_CACHE);
		this.exec = ExecUtils.newBoundedCachedThreadPool(C.IMAGE_LOADER_MAX_THREADS, LOG);

		this.intentExtras = getIntent().getExtras();
		this.inReplyToUid = this.intentExtras.getLong(ARG_IN_REPLY_TO_UID);
		this.inReplyToSid = this.intentExtras.getString(ARG_IN_REPLY_TO_SID);
		this.alsoMentions = this.intentExtras.getStringArray(ARG_ALSO_MENTIONS);
		LOG.i("inReplyToUid=%d inReplyToSid=%s alsoMentions=%s", this.inReplyToUid, this.inReplyToSid, Arrays.toString(this.alsoMentions));

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
			askAccount();
			this.askAccountOnActivate = false;
		}
	}

	@Override
	protected void onSaveInstanceState (final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(ARG_ACCOUNT_ID, getSelectedAccount().getId());
		this.enabledPostToAccounts.addToBundle(outState);
		outState.putParcelable(ARG_ATTACHMENT, this.attachment);
		outState.putParcelable(ARG_TMP_ATTACHMENT, this.tmpAttachment);
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

	private void setupAccounts (final Bundle savedInstanceState, final Collection<Account> accounts, final Config conf) {
		String accountId = null;
		Account account = null;

		if (savedInstanceState != null) accountId = savedInstanceState.getString(ARG_ACCOUNT_ID);
		if (accountId == null) accountId = this.intentExtras.getString(ARG_ACCOUNT_ID);
		if (accountId != null) {
			account = conf.getAccount(accountId);
			final List<String> svcs = this.intentExtras.getStringArrayList(ARG_SVCS);
			LOG.i("accountId=%s svcs=%s", account.getId(), svcs);

			this.enabledPostToAccounts.setAccount(account);
			this.enabledPostToAccounts.fromBundle(savedInstanceState);
			if (svcs != null && !this.enabledPostToAccounts.isServicesPreSpecified()) {
				for (String svc : svcs) {
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
		if (this.attachment == null) this.attachment = this.intentExtras.getParcelable(ARG_ATTACHMENT);
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
		((Button) findViewById(R.id.btnAttach)).setOnClickListener(this.attachClickListener);
	}

	private void setupTextBody () {
		this.txtBody = (EditText) findViewById(R.id.txtBody);
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

	private void wireMainButtons () {
		((Button) findViewById(R.id.btnCancel)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (final View v) {
				finish();
			}
		});

		((Button) findViewById(R.id.btnPost)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (final View v) {
				askPost(false);
			}
		});

		if (Config.isConfigFilePresent()) ((Button) findViewById(R.id.btnPost)).setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick (final View v) {
				askPost(true);
				return true;
			}
		});
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

	protected void askAccount () {
		final List<Account> items = readAccountsOrAlert();
		if (items == null) return;
		DialogHelper.askItem(this, "Post to Account", items, new Listener<Account>() {
			@Override
			public void onAnswer (final Account account) {
				setSelectedAccount(account);
			}
		});
	}

	private List<Account> readAccountsOrAlert () {
		try {
			return new ArrayList<Account>(this.prefs.readAccounts());
		}
		catch (final JSONException e) {
			DialogHelper.alert(this, "Failed to read accounts.", e);
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
				((TextView) view.findViewById(R.id.tweetDetailDate)).setText(DateFormat.getDateTimeInstance().format(new Date(TimeUnit.SECONDS.toMillis(tweet.getTime()))));
			}
		}
		initBody(tweet);
		this.txtBody.setSelection(this.txtBody.getText().length());
	}

	private void setPostToAccountExclusive (final ServiceRef svc) {
		if (svc == null) return;
		LOG.d("setPostToAccountExclusive(%s)", svc);
		this.enabledPostToAccounts.enableExclusiveAndSetPreSpecified(svc);
		PostToAccountLoaderTask.setAccountBtns(this.llSubAccounts, this.enabledPostToAccounts);
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

	protected void askPost (final boolean viaOutbox) {
		final Account account = getSelectedAccount();
		final Set<ServiceRef> svcs = this.enabledPostToAccounts.copyOfServices();
		final AlertDialog.Builder dlgBld = new AlertDialog.Builder(this);

		String msg;
		switch (account.getProvider()) {
			case SUCCESSWHALE:
			case BUFFER:
				msg = String.format("Post to these %s accounts?%n%s", account.getUiTitle(), ServiceRef.humanList(svcs, "\n"));
				break;
			default:
				msg = String.format("Post to %s?", account.getUiTitle());
		}
		if (viaOutbox) msg = "WARNING: posting via outbox is a BETA feature desu~\n\n" + msg;
		dlgBld.setMessage(msg);

		dlgBld.setPositiveButton("Post", new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				if (viaOutbox) {
					submitPostToOutput(account, svcs);
				}
				else {
					submitPost(account, svcs);
				}
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
				.putExtra(ARG_ATTACHMENT, this.attachment)
				.putExtra(ARG_BODY, body);

		final ArrayList<String> svcsLst = new ArrayList<String>();
		for (ServiceRef svc : svcs) {
			svcsLst.add(svc.toServiceMeta());
		}
		recoveryIntent.putStringArrayListExtra(ARG_SVCS, svcsLst);

		new PostTask(getApplicationContext(), new PostRequest(account, svcs, body, this.inReplyToSid, this.attachment, recoveryIntent)).execute();
		finish();
	}

	protected void submitPostToOutput(final Account account, final Set<ServiceRef> svcs) {
		DialogHelper.alert(this, "TODO: submit to outbox no implemented.");
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
		txtAttached.setText(String.format("Attachment: %s", metadata.getUiTitle()));
		txtAttached.setVisibility(metadata.exists() ? View.VISIBLE : View.GONE);
		return metadata;
	}

	private final OnClickListener attachClickListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			showAttachMenu(v);
		}
	};

	protected void showAttachMenu (final View v) {
		if (this.attachment == null) {
			askChoosePicture();
		}
		else {
			final PopupMenu popupMenu = new PopupMenu(this, v);
			popupMenu.getMenuInflater().inflate(R.menu.attachmenu, popupMenu.getMenu());
			popupMenu.setOnMenuItemClickListener(this.attachMenuClickListener);
			popupMenu.show();
		}
	}

	protected final PopupMenu.OnMenuItemClickListener attachMenuClickListener = new PopupMenu.OnMenuItemClickListener() {
		@Override
		public boolean onMenuItemClick (final MenuItem item) {
			return attachMenuItemClick(item);
		}
	};

	protected boolean attachMenuItemClick (final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.mnuChange:
				askChoosePicture();
				return true;
			case R.id.mnuShrink:
				showShrinkPictureDlg();
				return true;
			case R.id.mnuRemove:
				this.attachment = null;
				redrawAttachment();
				return true;
			default:
				return false;
		}
	}

	private void askChoosePicture () {
		this.tmpAttachment = Uri.fromFile(AttachmentStorage.getExternalTempFile(".jpg"));
		startActivityForResult(Intent.createChooser(
				new Intent(Intent.ACTION_GET_CONTENT)
						.setType("image/*")
						.addCategory(Intent.CATEGORY_OPENABLE),
				"Select Picture")
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
					dlg.recycle();
				}
			});
			dlgBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick (final DialogInterface dialog, final int whichButton) {
					dialog.cancel();
					dlg.recycle();
				}
			});
			dlgBuilder.create().show();
		}
		catch (IOException e) {
			DialogHelper.alert(this, e);
		}
	}

	protected void resizeAttachedPicture (final PictureResizeDialog dlg) {
		try {
			this.attachment = dlg.resizeToTempFile();
			redrawAttachment();
		}
		catch (IOException e) {
			DialogHelper.alert(this, e);
		}
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
				DialogHelper.alert(this, "Media is missing from response intent.");
				return;
			}
			if (ImageMetadata.isUnderstoodResource(uri)) {
				this.attachment = uri;
				final ImageMetadata metadata = redrawAttachment();
				if (metadata.getSize() > PROMPT_RESIZE_MIN_SIZE) askShrinkPicture(metadata);
			}
			else {
				DialogHelper.alert(this, "Unknown resource:\n" + uri);
			}
		}
		catch (IOException e) {
			DialogHelper.alert(this, e);
		}
	}

	private void askShrinkPicture (final ImageMetadata metadata) {
		DialogHelper.askYesNo(this,
				"Picture is " + IoHelper.readableFileSize(metadata.getSize())
				, "Shrink...", "Full Size", new Runnable() {
					@Override
					public void run () {
						showShrinkPictureDlg();
					}
				});
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
