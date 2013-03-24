package com.vaguehope.onosendai.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Config;

public class PostActivity extends Activity {

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

		Bundle extras = getIntent().getExtras();
		int columnId = extras.getInt(TweetListFragment.ARG_COLUMN_ID, -1);

		Spinner spnAccount = (Spinner) findViewById(R.id.spnAccount);
		spnAccount.setAdapter(new AccountAdaptor(getApplicationContext(), conf));
		// TODO use columnId to set initial account.
	}

	private static class AccountAdaptor extends BaseAdapter {

		private final LayoutInflater layoutInflater;
		private final List<Account> accounts;

		public AccountAdaptor (final Context context, final Config conf) {
			this.layoutInflater = LayoutInflater.from(context);
			this.accounts = new ArrayList<Account>(conf.getAccounts().values());
		}

		@Override
		public int getCount () {
			return this.accounts.size();
		}

		@Override
		public Object getItem (final int position) {
			return this.accounts.get(position);
		}

		@Override
		public long getItemId (final int position) {
			return position;
		}

		@Override
		public View getView (final int position, final View convertView, final ViewGroup parent) {
			View view = convertView;
			RowView rowView;
			if (view == null) {
				view = this.layoutInflater.inflate(R.layout.accountlistrow, null);
				rowView = new RowView(
						(TextView) view.findViewById(R.id.txtMain)
						);
				view.setTag(rowView);
			}
			else {
				rowView = (RowView) view.getTag();
			}

			Account item = this.accounts.get(position);
			rowView.getMain().setText(item.id);

			return view;
		}

		private static class RowView {

			private final TextView main;

			public RowView (final TextView main) {
				this.main = main;
			}

			public TextView getMain () {
				return this.main;
			}

		}

	}

}
