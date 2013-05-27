package com.vaguehope.onosendai.ui.pref;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.util.CollectionHelper;
import com.vaguehope.onosendai.util.EqualHelper;
import com.vaguehope.onosendai.util.StringHelper;

class ColumnDialog {

	private static final List<Duration> REFRESH_DURAITONS = CollectionHelper.listOf(
			new Duration(0, "Never"),
			new Duration(15, "15 minutes"),
			new Duration(30, "30 minutes"),
			new Duration(45, "45 minutes"),
			new Duration(60, "1 hour"),
			new Duration(60 * 2, "2 hours"),
			new Duration(60 * 3, "3 hours"),
			new Duration(60 * 6, "6 hours"),
			new Duration(60 * 12, "12 hours"),
			new Duration(60 * 24, "24 hours")
			);

	private final int id;
	private final String accountId;
	private final Column initialValue;

	private final View llParent;
	private final EditText txtTitle;
	private final Spinner spnPosition;
	private final EditText txtResource;
	private final Spinner spnRefresh;
	private final CheckBox chkNotify;
	private final CheckBox chkDelete;

	public ColumnDialog (final Context context, final Prefs prefs, final int id, final String accountId) {
		this(context, prefs, id, accountId, null);
	}

	public ColumnDialog (final Context context, final Prefs prefs, final Column initialValue) {
		this(context, prefs,
				initialValue != null ? initialValue.getId() : null,
				initialValue != null ? initialValue.getAccountId() : null,
				initialValue);
	}

	private ColumnDialog (final Context context, final Prefs prefs, final int id, final String accountId, final Column initialValue) {
		if (prefs == null) throw new IllegalArgumentException("Prefs can not be null.");
		if (initialValue != null && initialValue.getId() != id) throw new IllegalStateException("ID and initialValue ID do not match.");
		if (initialValue != null && !EqualHelper.equal(initialValue.getAccountId(), accountId)) throw new IllegalStateException("Account ID and initialValue account ID do not match.");

		this.id = id;
		this.accountId = accountId;
		this.initialValue = initialValue;

		LayoutInflater inflater = LayoutInflater.from(context);
		this.llParent = inflater.inflate(R.layout.columndialog, null);

		this.txtTitle = (EditText) this.llParent.findViewById(R.id.txtTitle);
		this.spnPosition = (Spinner) this.llParent.findViewById(R.id.spnPosition);
		this.txtResource = (EditText) this.llParent.findViewById(R.id.txtResource);
		this.spnRefresh = (Spinner) this.llParent.findViewById(R.id.spnRefresh);
		this.chkNotify = (CheckBox) this.llParent.findViewById(R.id.chkNotify);
		this.chkDelete = (CheckBox) this.llParent.findViewById(R.id.chkDelete);

		final ArrayAdapter<Integer> posAdapter = new ArrayAdapter<Integer>(context, R.layout.numberspinneritem);
		posAdapter.addAll(CollectionHelper.sequence(1, prefs.readColumnIds().size() + (initialValue == null ? 1 : 0)));
		this.spnPosition.setAdapter(posAdapter);

		final ArrayAdapter<Duration> refAdapter = new ArrayAdapter<Duration>(context, R.layout.numberspinneritem);
		refAdapter.addAll(REFRESH_DURAITONS);
		this.spnRefresh.setAdapter(refAdapter);

		this.chkDelete.setChecked(false);

		if (initialValue != null) {
			this.txtTitle.setText(initialValue.getTitle());
			this.spnPosition.setSelection(posAdapter.getPosition(Integer.valueOf(prefs.readColumnPosition(initialValue.getId()) + 1)));
			this.txtResource.setText(initialValue.getResource());
			setDurationSpinner(initialValue.getRefreshIntervalMins(), refAdapter);
			if (StringHelper.isEmpty(accountId)) this.spnRefresh.setEnabled(false);
			this.chkNotify.setChecked(initialValue.isNotify());
			this.chkDelete.setVisibility(View.VISIBLE);
		}
		else {
			this.spnPosition.setSelection(posAdapter.getCount() - 1); // Last item.
			setDurationSpinner(0, refAdapter); // Default to no background refresh.
			this.chkDelete.setVisibility(View.GONE);
		}
	}

	private void setDurationSpinner (final int mins, final ArrayAdapter<Duration> refAdapter) {
		final Duration duration = new Duration(mins > 0 ? mins : 0);
		final int pos = refAdapter.getPosition(duration);
		if (pos < 0) refAdapter.add(duration); // Allow for any value to have been chosen before.
		this.spnRefresh.setSelection(pos < 0 ? refAdapter.getPosition(duration) : pos);
	}

	public Column getInitialValue () {
		return this.initialValue;
	}

	public View getRootView () {
		return this.llParent;
	}

	public void setResource (final String resource) {
		this.txtResource.setText(resource);
	}

	public void setTitle (final String title) {
		this.txtTitle.setText(title);
	}

	/**
	 * 0 based.
	 */
	public int getPosition () {
		return ((Integer) this.spnPosition.getSelectedItem()) - 1;
	}

	public boolean isDeleteSelected () {
		return this.chkDelete.isChecked();
	}

	public Column getValue () {
		return new Column(this.id,
				this.txtTitle.getText().toString(),
				this.accountId,
				this.txtResource.getText().toString(),
				((Duration) this.spnRefresh.getSelectedItem()).getMins(),
				this.initialValue != null ? this.initialValue.getExcludeColumnIds() : null, // TODO GUI for excludes.
				this.chkNotify.isChecked());
	}

	private static class Duration {

		private final int mins;
		private final String title;

		public Duration (final int mins) {
			this(mins, null);
		}

		public Duration (final int mins, final String title) {
			this.mins = mins;
			this.title = title;
		}

		public int getMins () {
			return this.mins;
		}

		@Override
		public String toString () {
			return this.title;
		}

		@Override
		public boolean equals (final Object o) {
			if (o == null) return false;
			if (o == this) return true;
			if (!(o instanceof Duration)) return false;
			final Duration that = (Duration) o;
			return this.mins == that.mins;
		}

		@Override
		public int hashCode () {
			return this.mins;
		}

	}

}
