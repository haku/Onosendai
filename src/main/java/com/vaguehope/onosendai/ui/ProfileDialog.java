package com.vaguehope.onosendai.ui;

import java.util.concurrent.Executor;

import twitter4j.Relationship;
import twitter4j.TwitterException;
import twitter4j.User;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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

public class ProfileDialog {

	private static final String PROFILE_URL_TEMPLATE = "https://twitter.com/%s";
	private static final LogWrapper LOG = new LogWrapper("PD");

	public static void show (final Context context, final Executor executor, final ImageLoader imageLoader, final Account account, final String username) {
		final ProfileDialog pDlg = new ProfileDialog(context, imageLoader, account, username);
		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(context);
		dlgBuilder.setView(pDlg.getRootView());
		final AlertDialog dlg = dlgBuilder.create();
		dlg.show();
		new FetchUserTask(pDlg, account, username).executeOnExecutor(executor);
	}

	private final Context context;
	private final ImageLoader imageLoader;
	private final Account account;

	private final View parentView;
	private final View lUser;
	private final ImageView imgAvatar;
	private final TextView txtFullname;
	private final TextView txtUsername;
	private final TextView txtDescription;
	private final Button btnProfile;
	private final Button btnFollowUnfollow;

	private ProfileDialog (final Context context, final ImageLoader imageLoader, final Account account, final String username) {
		this.context = context;
		this.imageLoader = imageLoader;
		this.account = account;
		this.parentView = LayoutInflater.from(context).inflate(R.layout.profile, null);

		this.lUser = this.parentView.findViewById(R.id.lUser);
		this.imgAvatar = (ImageView) this.parentView.findViewById(R.id.imgAvatar);
		this.txtFullname = (TextView) this.parentView.findViewById(R.id.txtFullname);
		this.txtUsername = (TextView) this.parentView.findViewById(R.id.txtUsername);
		this.txtDescription = (TextView) this.parentView.findViewById(R.id.txtDescription);
		this.btnProfile = (Button) this.parentView.findViewById(R.id.btnProfile);
		this.btnFollowUnfollow = (Button) this.parentView.findViewById(R.id.btnFollowUnfollow);

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
		if (user.isFollowRequestSent()) this.btnFollowUnfollow.setText("Follow requested");
	}

	public void showRelationship (final Relationship rel, final User targetUser) {
		final UserActionTask.UserAction action;
		if (rel.isSourceFollowingTarget()) {
			this.btnFollowUnfollow.setText("Unfollow");
			action = UserActionTask.UserAction.UNFOLLOW;
		}
		else {
			this.btnFollowUnfollow.setText("Follow");
			action = UserActionTask.UserAction.FOLLOW;
		}
		this.btnFollowUnfollow.setOnClickListener(new TaskClickListener(new UserActionTask(getContext(), this.account, action, targetUser)));
		this.btnFollowUnfollow.setEnabled(true);
	}

	private static class FetchUserTask extends AsyncTask<Void, Object, Exception> {

		private final ProfileDialog pDlg;
		private final Account account;
		private final String username;

		public FetchUserTask (final ProfileDialog pDlg, final Account account, final String username) {
			this.pDlg = pDlg;
			this.account = account;
			this.username = username;
		}

		@Override
		protected Exception doInBackground (final Void... params) {
			try {
				switch (this.account.getProvider()) {
					case TWITTER:
						fromTwitter();
						return null;
					default:
						return new UnsupportedOperationException("Currently show profile is only supported via Twitter accounts.");
				}
			}
			catch (final Exception e) {
				return e;
			}
		}

		private void fromTwitter () throws TwitterException {
			final TwitterProvider p = new TwitterProvider();
			try {
				final User otherUser = p.getUser(this.account, this.username);
				publishProgress(otherUser);
				if (!otherUser.isFollowRequestSent()) {
					publishProgress(p.getRelationship(this.account, otherUser));
				}
			}
			finally {
				p.shutdown();
			}
		}

		private User targetUser;

		@Override
		protected void onProgressUpdate (final Object... values) {
			for (final Object val : values) {
				if (val instanceof User) {
					this.targetUser = (User) val;
					this.pDlg.showUser(this.targetUser);
				}
				else if (val instanceof Relationship) {
					this.pDlg.showRelationship((Relationship) val, this.targetUser);
				}
			}
		}

		@Override
		protected void onPostExecute (final Exception e) {
			if (e != null) {
				LOG.e("Failed fetch user details.", e);
				DialogHelper.alert(this.pDlg.getContext(), e);
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

	private static class TaskClickListener implements OnClickListener {

		private final AsyncTask<?, ?, ?> task;

		public TaskClickListener (final AsyncTask<?, ?, ?> task) {
			this.task = task;
		}

		@Override
		public void onClick (final View v) {
			this.task.execute();
		}

	}

	private static class UserActionTask extends AsyncTask<Void, Void, Exception> {

		public enum UserAction {
			FOLLOW("Following"),
			UNFOLLOW("Unfollowing");
			private final String actionDesc;

			private UserAction (final String actionDesc) {
				this.actionDesc = actionDesc;
			}

			public String getActionDesc () {
				return this.actionDesc;
			}
		}

		private final Context context;
		private final Account account;
		private final UserAction action;
		private final User targetUser;

		private ProgressDialog dialog;

		public UserActionTask (final Context context, final Account account, final UserAction action, final User targetUser) {
			this.context = context;
			this.account = account;
			this.action = action;
			this.targetUser = targetUser;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(this.context, this.action.getActionDesc(), "Please wait...", true);
		}

		@Override
		protected Exception doInBackground (final Void... params) {
			try {
				switch (this.account.getProvider()) {
					case TWITTER:
						viaTwitter();
						return null;
					default:
						return new UnsupportedOperationException("Currently show profile is only supported via Twitter accounts.");
				}
			}
			catch (final Exception e) {
				return e;
			}
		}

		private void viaTwitter () throws TwitterException {
			final TwitterProvider p = new TwitterProvider();
			try {
				switch (this.action) {
					case FOLLOW:
						p.follow(this.account, this.targetUser);
						break;
					case UNFOLLOW:
						p.unfollow(this.account, this.targetUser);
						break;
					default:
						throw new UnsupportedOperationException("Unknown action: " + this.action);
				}
			}
			finally {
				p.shutdown();
			}
		}

		@Override
		protected void onPostExecute (final Exception result) {
			this.dialog.dismiss();
			if (result == null) {
				DialogHelper.alert(this.context, "Success.");
			}
			else {
				LOG.e("Failed.", result);
				DialogHelper.alert(this.context, result);
			}
		}

	}

}
