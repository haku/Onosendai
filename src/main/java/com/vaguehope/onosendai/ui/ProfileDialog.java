package com.vaguehope.onosendai.ui;

import java.util.concurrent.Executor;

import twitter4j.TwitterException;
import twitter4j.User;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.provider.twitter.TwitterProvider;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.Result;

public class ProfileDialog {

	private static final String PROFILE_URL_TEMPLATE = "https://twitter.com/%s";
	private static final LogWrapper LOG = new LogWrapper("PD");

	public static void show (final Context context, final Executor executor, final ImageLoader imageLoader, final Account account, final String username) {
		final ProfileDialog pDlg = new ProfileDialog(context, imageLoader, username);
		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(context);
		dlgBuilder.setView(pDlg.getRootView());
		final AlertDialog dlg = dlgBuilder.create();
		dlg.show();
		new FetchUserTask(pDlg, account, username).executeOnExecutor(executor);
	}

	private final Context context;
	private final ImageLoader imageLoader;
	private final View parentView;

	private final View lUser;
	private final ImageView imgAvatar;
	private final TextView txtFullname;
	private final TextView txtUsername;
	private final TextView txtDescription;
	private final Button btnProfile;

	private ProfileDialog (final Context context, final ImageLoader imageLoader, final String username) {
		this.context = context;
		this.imageLoader = imageLoader;
		this.parentView = LayoutInflater.from(context).inflate(R.layout.profile, null);

		this.lUser = this.parentView.findViewById(R.id.lUser);
		this.imgAvatar = (ImageView) this.parentView.findViewById(R.id.imgAvatar);
		this.txtFullname = (TextView) this.parentView.findViewById(R.id.txtFullname);
		this.txtUsername = (TextView) this.parentView.findViewById(R.id.txtUsername);
		this.txtDescription = (TextView) this.parentView.findViewById(R.id.txtDescription);
		this.btnProfile = (Button) this.parentView.findViewById(R.id.btnProfile);

		this.txtFullname.setText("...");
		this.txtUsername.setText(username);

		final String profileUrl = String.format(PROFILE_URL_TEMPLATE, username);
		this.btnProfile.setText(profileUrl);
		this.btnProfile.setOnClickListener(new GoToUrlClickListener(profileUrl));
	}

	public Context getContext () {
		return this.context;
	}

	private View getRootView () {
		return this.parentView;
	}

	public void showUser (final User user) {
		this.txtFullname.setText(user.getName());
		this.txtUsername.setText(user.getScreenName());
		this.txtDescription.setText(user.getDescription());
		this.imageLoader.loadImage(new ImageLoadRequest(user.getBiggerProfileImageURLHttps(), this.imgAvatar));
	}

	private static class FetchUserTask extends AsyncTask<Void, Void, Result<User>> {

		private final ProfileDialog pDlg;
		private final Account account;
		private final String username;

		public FetchUserTask (final ProfileDialog pDlg, final Account account, final String username) {
			this.pDlg = pDlg;
			this.account = account;
			this.username = username;
		}

		@Override
		protected Result<User> doInBackground (final Void... params) {
			try {
				switch (this.account.getProvider()) {
					case TWITTER:
						return fromTwitter();
					default:
						return new Result<User>(new UnsupportedOperationException("Currently show profile is only supported via Twitter accounts."));
				}
			}
			catch (final Exception e) {
				return new Result<User>(e);
			}
		}

		private Result<User> fromTwitter () throws TwitterException {
			final TwitterProvider p = new TwitterProvider();
			try {
				return new Result<User>(p.getUser(this.account, this.username));
			}
			finally {
				p.shutdown();
			}
		}

		@Override
		protected void onPostExecute (final Result<User> result) {
			if (result.isSuccess()) {
				this.pDlg.showUser(result.getData());
			}
			else {
				LOG.e("Failed fetch user details.", result.getE());
				DialogHelper.alert(this.pDlg.getContext(), result.getE());
			}
		}

	}

	private static class GoToUrlClickListener implements OnClickListener {

		private final String url;

		public GoToUrlClickListener (final String url) {
			this.url = url;
		}

		@Override
		public void onClick (final View v) {
			v.getContext().startActivity(new Intent(Intent.ACTION_VIEW)
					.setData(Uri.parse(this.url)));

		}

	}

}
