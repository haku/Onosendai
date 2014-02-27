package com.vaguehope.onosendai.storage;

import java.util.List;

import android.widget.ArrayAdapter;
import android.widget.Filter;

import com.vaguehope.onosendai.ui.PostActivity;
import com.vaguehope.onosendai.util.LogWrapper;

public class UsernameSearchAdapter extends ArrayAdapter<String> {

	protected static final LogWrapper LOG = new LogWrapper("UNS");

	private final Filter filter;

	public UsernameSearchAdapter (final PostActivity host) { // TODO be more specific than PostActivity.
		super(host, android.R.layout.simple_list_item_1);
		this.filter = new UsernameFilter(this, host);
	}

	@Override
	public Filter getFilter () {
		return this.filter;
	}

	private static class UsernameFilter extends Filter {

		private final ArrayAdapter<String> adapter;
		private final DbProvider dbProvider;

		public UsernameFilter (final ArrayAdapter<String> adapter, final DbProvider dbProvider) { // TODO be more specific than PostActivity.
			this.adapter = adapter;
			this.dbProvider = dbProvider;
		}

		@Override
		protected FilterResults performFiltering (final CharSequence constraint) {
			if (constraint == null) return new FilterResults();
			try {
				final String term = constraint.toString();
				final List<String> usernames = this.dbProvider.getDb().getUsernames(term, 10);

				final FilterResults filterResults = new FilterResults();
				filterResults.values = usernames;
				filterResults.count = usernames.size();
				return filterResults;
			}
			catch (final Exception e) { // NOSONAR Need to report errors.
				LOG.e("Search failed.", e);
				return new FilterResults();
			}
		}

		@Override
		protected void publishResults (final CharSequence constraint, final FilterResults results) {
			this.adapter.clear();
			if (results != null && results.count > 0) {
				final List<String> usernames = (List<String>) results.values;
				this.adapter.addAll(usernames);
				this.adapter.notifyDataSetChanged();
			}
			else {
				this.adapter.notifyDataSetInvalidated();
			}
		}

	}

}