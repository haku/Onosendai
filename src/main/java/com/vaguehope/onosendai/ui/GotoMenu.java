package com.vaguehope.onosendai.ui;

import java.util.List;

import android.os.AsyncTask;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.PopupMenu;

import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.storage.DbInterface;

public class GotoMenu implements OnClickListener {

	private static final int MNU_GOTO_BASE_ID = 100;

	private final MainActivity mainActivity;

	public GotoMenu (final MainActivity mainActivity) {
		this.mainActivity = mainActivity;
	}

	@Override
	public void onClick (final View v) {
		final PopupMenu mnu = new PopupMenu(this.mainActivity, v);
		final List<Column> columns = this.mainActivity.getConf().getColumns();
		int i = 0;
		for (final Column col : columns) {
			final MenuItem menuItem = mnu.getMenu().add(Menu.NONE, MNU_GOTO_BASE_ID + i, Menu.NONE, col.getTitle());
			final ScrollState scroll = this.mainActivity.getColumnScroll(col.getId());
			new UnreadCountLoaderTask(this.mainActivity.getDb(), col, menuItem, scroll).executeOnExecutor(this.mainActivity.getExec());
			i++;
		}
		mnu.setOnMenuItemClickListener(new GotoItemClientListener(columns, this.mainActivity));
		mnu.show();
	}

	private static class GotoItemClientListener implements PopupMenu.OnMenuItemClickListener {

		private final List<Column> columns;
		private final MainActivity mainActivity;

		public GotoItemClientListener (final List<Column> columns, final MainActivity mainActivity) {
			this.columns = columns;
			this.mainActivity = mainActivity;
		}

		@Override
		public boolean onMenuItemClick (final MenuItem item) {
			final int page = item.getItemId() - MNU_GOTO_BASE_ID;
			if (page >= 0 && page < this.columns.size()) {
				this.mainActivity.gotoPage(page);
				return true;
			}
			return false;
		}

	}

	private static class UnreadCountLoaderTask extends AsyncTask<Void, Void, Integer> {

		private final DbInterface db;
		private final Column column;
		private final MenuItem menuItem;
		private final ScrollState scroll;

		public UnreadCountLoaderTask (final DbInterface db, final Column column, final MenuItem menuItem, final ScrollState scroll) {
			this.db = db;
			this.column = column;
			this.menuItem = menuItem;
			this.scroll = scroll;
		}

		@Override
		protected Integer doInBackground (final Void... params) {
			return this.db.getScrollUpCount(this.column.getId(), this.column.getExcludeColumnIds(), this.scroll);
		}

		@Override
		protected void onPostExecute (final Integer count) {
			if (count != 0) this.menuItem.setTitle(this.menuItem.getTitle() + " (" + count + ")");
		}

	}

}
