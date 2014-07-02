package com.vaguehope.onosendai.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;
import com.vaguehope.onosendai.util.TextFilter;

public class TextFiltersDialog {

	public static void show (final Context context, final String inputText, final Listener<String> onFilteredText) {
		final TextFiltersDialog dlg = new TextFiltersDialog(context, inputText);
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setView(dlg.getRootView());
		builder.setNegativeButton(android.R.string.cancel, DialogHelper.DLG_CANCEL_CLICK_LISTENER);
		builder.setPositiveButton(android.R.string.ok, new android.content.DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				onFilteredText.onAnswer(dlg.getOutput());
				dialog.dismiss();
			}
		});
		builder.show();
	}

	private final Context context;
	private final View llParent;
	private final String inputText;
	private final TextView txtPreview;
	private final Spinner spnFilters;

	public TextFiltersDialog (final Context context, final String inputText) {
		this.context = context;
		this.inputText = inputText;
		final LayoutInflater inflater = LayoutInflater.from(context);
		this.llParent = inflater.inflate(R.layout.textfilters, null);
		this.txtPreview = ((TextView) this.llParent.findViewById(R.id.txtPreview));

		this.spnFilters = ((Spinner) this.llParent.findViewById(R.id.spnFilters));
		final ArrayAdapter<TextFilter> filtersAdapter = new ArrayAdapter<TextFilter>(context, R.layout.numberspinneritem);
		filtersAdapter.addAll(TextFilter.values());
		this.spnFilters.setAdapter(filtersAdapter);
		this.spnFilters.setOnItemSelectedListener(this.spnChangeListener);
	}

	private View getRootView () {
		return this.llParent;
	}

	protected String getOutput () {
		final TextFilter filter = (TextFilter) this.spnFilters.getSelectedItem();
		if (filter == null) return this.inputText;
		return filter.apply(this.inputText);
	}

	private final OnItemSelectedListener spnChangeListener = new OnItemSelectedListener() {
		@Override
		public void onItemSelected (final AdapterView<?> parent, final View view, final int position, final long id) {
			updatePreview();
		}

		@Override
		public void onNothingSelected (final AdapterView<?> arg0) {/**/}
	};

	protected void updatePreview () {
		try {
			this.txtPreview.setText(getOutput());
		}
		catch (final Exception e) {
			DialogHelper.alert(this.context, e);
		}
	}

}
