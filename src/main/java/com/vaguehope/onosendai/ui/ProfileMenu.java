package com.vaguehope.onosendai.ui;

import android.content.Intent;
import android.net.Uri;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.provider.twitter.TwitterUrls;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;
import com.vaguehope.onosendai.util.Titleable;

public class ProfileMenu {

	private static abstract class MenuAction implements Titleable {

		private final String title;

		public MenuAction (final String title) {
			this.title = title;
		}

		@Override
		public String getUiTitle () {
			return this.title;
		}

		public abstract void onClick ();

	}

	public static void show (final MainActivity mainActivity, final ImageLoader imageLoader, final Account account, final String username, final String fullName) {
		final MenuAction profile = new MenuAction("Profile") {
			@Override
			public void onClick () {
				ProfileDialog.show(mainActivity, imageLoader, account, username);
			}
		};

		final String profileUrl = TwitterUrls.profileUrl(username);
		final MenuAction userLink = new MenuAction(profileUrl) {
			@Override
			public void onClick () {
				mainActivity.startActivity(new Intent(Intent.ACTION_VIEW)
						.setData(Uri.parse(profileUrl)));
			}
		};

		final MenuAction tweets = new MenuAction("Tweets") {
			@Override
			public void onClick () {
				mainActivity.showLocalSearch(String.format("u:%s", username));
			}
		};

		final MenuAction[] actions = new MenuAction[] { profile, userLink, tweets };

		DialogHelper.askItem(mainActivity, fullName, actions, new Listener<MenuAction>() {
			@Override
			public void onAnswer (final MenuAction answer) {
				answer.onClick();
			}
		});
	}
}
