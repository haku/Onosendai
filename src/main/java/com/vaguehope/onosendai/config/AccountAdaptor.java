package com.vaguehope.onosendai.config;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.vaguehope.onosendai.R;

public class AccountAdaptor extends BaseAdapter {

	private final LayoutInflater layoutInflater;
	private final List<Account> accounts;

	public AccountAdaptor (final Context context, final Config conf) {
		this.layoutInflater = LayoutInflater.from(context);
		this.accounts = new ArrayList<Account>(conf.getAccounts().values());
	}

	public int getAccountPosition (final Account account) {
		return this.accounts.indexOf(account);
	}

	public Account getAccount (final int position) {
		return this.accounts.get(position);
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
		AccountAdaptor.AccountRowView rowView;
		if (view == null) {
			view = this.layoutInflater.inflate(R.layout.accountlistrow, null);
			rowView = new AccountRowView(
					(TextView) view.findViewById(R.id.txtMain)
					);
			view.setTag(rowView);
		}
		else {
			rowView = (AccountAdaptor.AccountRowView) view.getTag();
		}

		Account account = this.accounts.get(position);
		rowView.getMain().setText(account.getUiTitle());

		return view;
	}

	private static class AccountRowView {

		private final TextView main;

		public AccountRowView (final TextView main) {
			this.main = main;
		}

		public TextView getMain () {
			return this.main;
		}

	}

}