package com.vaguehope.onosendai.ui.pref;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.config.ColumnFeed;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.ui.pref.ColumnChooser.ColumnChoiceListener;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;
import com.vaguehope.onosendai.util.EqualHelper;
import com.vaguehope.onosendai.util.Titleable;

public class ColumFeedDialog implements Titleable {

	private final Context context;
	private final Prefs prefs;
	private final Account account;

	private String resource;
	private String title;

	private final View llParent;
	private final Button btnResource;

	public ColumFeedDialog (final Context context, final Prefs prefs, final ColumnFeed initialValue, final Account account) {
		if (initialValue != null && !EqualHelper.equal(initialValue.getAccountId(), account != null ? account.getId() : null))
			throw new IllegalArgumentException("ColumnFeed does not match Account: " + initialValue + " vs " + account + ".");
		this.context = context;
		this.prefs = prefs;
		this.account = account;

		final LayoutInflater inflater = LayoutInflater.from(context);
		this.llParent = inflater.inflate(R.layout.column_feed_dialog, null);

		this.btnResource = (Button) this.llParent.findViewById(R.id.btnResource);
		this.btnResource.setOnClickListener(this.btnResourceClickListener);
		this.btnResource.setOnLongClickListener(this.btnResourceLongClickListener);

		if (initialValue != null) {
			setResource(initialValue.getResource());
		}
	}

	@Override
	public String getUiTitle () {
		return "Feed";
	}

	public View getRootView () {
		return this.llParent;
	}

	public void setResource (final String resource) {
		this.resource = resource;
		this.btnResource.setText(String.valueOf(resource).replace(':', '\n'));
		final AccountProvider provider = this.account != null ? this.account.getProvider() : null;
		this.btnResource.setEnabled(provider == AccountProvider.TWITTER || provider == AccountProvider.SUCCESSWHALE);
	}

	public void setTitle (final String title) {
		this.title = title;
	}

	public ColumnFeed getValue () {
		return new ColumnFeed(this.account.getId(), this.resource);
	}

	public String getTitle () {
		return this.title;
	}

	private final OnClickListener btnResourceClickListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			btnResourceClick();
		}
	};

	private final OnLongClickListener btnResourceLongClickListener = new OnLongClickListener() {
		@Override
		public boolean onLongClick (final View v) {
			btnResourceLongClick();
			return true;
		}
	};

	protected void btnResourceClick () {
		new ColumnChooser(this.context, this.prefs, new ColumnChoiceListener() {
			@Override
			public void onColumn (final Account unused, final String newResource, final String newTitle) {
				if (newResource != null) setResource(newResource);
				if (newTitle != null) setTitle(newTitle);
			}
		}).promptAddColumn(this.account, this.resource);
	}

	protected void btnResourceLongClick () {
		DialogHelper.askString(this.context, "Resource:", this.resource, true, false, new Listener<String>() { //ES
			@Override
			public void onAnswer (final String newResource) {
				if (newResource != null) setResource(newResource);
			}
		});
	}

}
