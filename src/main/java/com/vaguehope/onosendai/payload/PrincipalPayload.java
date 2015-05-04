package com.vaguehope.onosendai.payload;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.util.DialogHelper;

public class PrincipalPayload extends Payload {

	private final DateFormat dateFormat = DateFormat.getDateTimeInstance();

	public PrincipalPayload (final Tweet tweet) {
		super(tweet, null, PayloadType.PRINCIPAL);
	}

	@Override
	public String getTitle () {
		return String.format("tweet[%s]", getOwnerTweet().getSid());
	}

	@Override
	public PayloadLayout getLayout () {
		return PayloadLayout.PRINCIPAL_TWEET;
	}

	@Override
	public PayloadRowView makeRowView (final View view) {
		final TextView txtBody = (TextView) view.findViewById(R.id.tweetDetailBody);
		txtBody.setCustomSelectionActionModeCallback(new Callback() {
			private static final int ID_ADD_FILTER = 100;
			@Override
			public boolean onCreateActionMode (final ActionMode mode, final Menu menu) {
				final MenuItem mnuAddFilter = menu.add(Menu.NONE, ID_ADD_FILTER, Menu.NONE, "Add Filter");
				mnuAddFilter.setIcon(R.drawable.exclamation_red);
				return true;
			}

			@Override
			public boolean onPrepareActionMode (final ActionMode mode, final Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked (final ActionMode mode, final MenuItem item) {
				switch (item.getItemId()) {
					case ID_ADD_FILTER:
						final CharSequence selStr = txtBody.getText().subSequence(txtBody.getSelectionStart(), txtBody.getSelectionEnd());
						DialogHelper.alert(view.getContext(), "TODO add filter:\n" + selStr);
						return true;
					default:
						return false;
				}
			}

			@Override
			public void onDestroyActionMode (final ActionMode mode) {}
		});
		return new PayloadRowView(
				txtBody,
				(ImageView) view.findViewById(R.id.tweetDetailAvatar),
				(TextView) view.findViewById(R.id.tweetDetailName),
				(TextView) view.findViewById(R.id.tweetDetailDate));
	}

	@Override
	public void applyTo (final PayloadRowView rowView, final ImageLoader imageLoader, final int reqWidth, final PayloadClickListener clickListener) {
		final Tweet tweet = getOwnerTweet();

		rowView.setText(tweet.getBody());
		rowView.setSecondaryText(tweet.getFullnameWithSubtitle());

		final Meta postTimeMeta = tweet.getFirstMetaOfType(MetaType.POST_TIME);
		final long tweetTime = postTimeMeta != null ? postTimeMeta.toLong(0L) : tweet.getTime();
		rowView.setTertiaryText(this.dateFormat.format(new Date(TimeUnit.SECONDS.toMillis(tweetTime))));

		final String avatarUrl = tweet.getAvatarUrl();
		if (avatarUrl != null) {
			imageLoader.loadImage(new ImageLoadRequest(avatarUrl, rowView.getImage()));
		}
		else {
			rowView.getImage().setImageResource(R.drawable.question_blue);
		}
	}

}
