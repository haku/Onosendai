package com.vaguehope.onosendai.model;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.util.StringHelper;

public class OutboxAdapter extends BaseAdapter {

	private final LayoutInflater layoutInflater;
	private final Config conf;

	private List<OutboxTweet> listData;

	public OutboxAdapter (final Context context, final Config config) {
		this.conf = config;
		this.layoutInflater = LayoutInflater.from(context);
	}

	public void setInputData (final List<OutboxTweet> data) {
		this.listData = data;
		notifyDataSetChanged();
	}

	public List<OutboxTweet> getInputData () {
		return this.listData;
	}

	@Override
	public int getCount () {
		return this.listData == null ? 0 : this.listData.size();
	}

	@Override
	public Object getItem (final int position) {
		return getTweet(position);
	}

	@Override
	public long getItemId (final int position) {
		final OutboxTweet tweet = getTweet(position);
		if (tweet == null) return -1;
		return tweet.getUid();
	}

	public OutboxTweet getTweet (final int position) {
		if (this.listData == null) return null;
		if (position >= this.listData.size()) return null;
		return this.listData.get(position);
	}

	@Override
	public View getView (final int position, final View convertView, final ViewGroup parent) {
		View view = convertView;
		RowView rowView;
		if (view == null) {
			view = this.layoutInflater.inflate(R.layout.outboxrow, null);
			rowView = new RowView(view);
			view.setTag(rowView);
		}
		else {
			rowView = (RowView) view.getTag();
		}
		rowView.setItem(this.listData.get(position), this.conf);
		return view;
	}

	private static class RowView {

		private static final int MAX_ERROR_MSG_CHARS = 150;

		private final TextView body;
		private final TextView account;
		private final TextView status;

		public RowView (final View view) {
			this((TextView) view.findViewById(R.id.txtBody),
					(TextView) view.findViewById(R.id.txtAccount),
					(TextView) view.findViewById(R.id.txtStatus));
		}

		public RowView (final TextView body, final TextView account, final TextView status) {
			this.body = body;
			this.account = account;
			this.status = status;
		}

		public void setItem (final OutboxTweet item, final Config conf) {
			this.body.setText(item.getBody());
			this.account.setText(summariseAccount(item, conf));
			this.status.setText(summariseStatus(item));
		}

		private static String summariseAccount (final OutboxTweet item, final Config conf) {
			final StringBuilder s = new StringBuilder();
			final Account account = conf.getAccount(item.getAccountId());
			if (account != null) {
				s.append(account.getUiTitle());
			}
			else {
				s.append(item.getAccountId());
			}
			if (item.getAttachment() != null) s.append(", with picture.");
			return s.toString();
		}

		private static String summariseStatus (final OutboxTweet item) {
			final StringBuilder s = new StringBuilder(String.valueOf(item.getStatus()));
			if (item.getAttemptCount() > 0) s.append(", ").append(item.getAttemptCount()).append(" failures."); //ES
			if (item.getLastError() != null) s.append("\n").append(StringHelper.maxLength(item.getLastError(), MAX_ERROR_MSG_CHARS));
			return s.toString();
		}
	}

}
