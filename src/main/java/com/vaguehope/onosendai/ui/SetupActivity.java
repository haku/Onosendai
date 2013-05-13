package com.vaguehope.onosendai.ui;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Config;

public class SetupActivity extends Activity {

	private ArrayAdapter<SetupAction> actAdaptor;
	private Spinner spnAct;
	private TextView txtActDes;
	private Button btnContinue;

	@Override
	protected void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setup);

		this.spnAct = (Spinner) findViewById(R.id.spnSetupAction);
		this.txtActDes = (TextView) findViewById(R.id.txtActionDescription);
		this.btnContinue = (Button) findViewById(R.id.btnContinue);

		this.actAdaptor = new ArrayAdapter<SetupAction>(this, R.layout.setupactionlistrow);
		this.actAdaptor.add(SetupActionType.SWIMPORT.toSetupAction(this));
		this.actAdaptor.add(SetupActionType.WRITETEMPLATE.toSetupAction(this));

		this.spnAct.setAdapter(this.actAdaptor);
		this.spnAct.setOnItemSelectedListener(this.actOnItemSelectedListener);

		if (Config.isTemplateConfigured()) {
			final SetupAction useTemplate = SetupActionType.USETEMPLATE.toSetupAction(this);
			this.actAdaptor.add(useTemplate);
			this.spnAct.setSelection(this.actAdaptor.getPosition(useTemplate));
		}

		this.btnContinue.setOnClickListener(this.btnContinueListener);
	}

	protected ArrayAdapter<SetupAction> getActAdaptor () {
		return this.actAdaptor;
	}

	protected void actionSelected (final SetupAction act) {
		this.txtActDes.setText(act.getDescription());
	}

	protected void runAction () {
		final SetupAction act = (SetupAction) this.spnAct.getSelectedItem();
		switch (act.getType()) {
			case SWIMPORT:
				doSwImport();
				break;
			case WRITETEMPLATE:
				doWriteTemplate();
				break;
			case USETEMPLATE:
				doUseTemplate();
				break;
			default:
				Toast.makeText(this, "TODO: " + act.getType(), Toast.LENGTH_SHORT).show();
		}
	}

	private final OnItemSelectedListener actOnItemSelectedListener = new OnItemSelectedListener() {
		@Override
		public void onItemSelected (final AdapterView<?> parent, final View view, final int position, final long id) {
			actionSelected(getActAdaptor().getItem(position));
		}

		@Override
		public void onNothingSelected (final AdapterView<?> arg0) {/**/}
	};

	private final OnClickListener btnContinueListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			runAction();
		}
	};

	private void doWriteTemplate () {
		try {
			final File f = Config.writeTemplateConfig();
			alertAndClose(this, "Template file written to: " + f.getAbsolutePath());
		}
		catch (final Exception e) { // NOSONAR show user all errors.
			alertAndClose(this, e);
		}
	}

	private void doUseTemplate () {
		try {
			Config.useTemplateConfig();
			startActivity(new Intent(getApplicationContext(), MainActivity.class));
			finish();
		}
		catch (final Exception e) { // NOSONAR show user all errors.
			alertAndClose(this, e);
		}
	}

	private void doSwImport () {
		final ViewGroup llSetupType = (ViewGroup) findViewById(R.id.llSetupType);
		llSetupType.setVisibility(View.GONE);

		final ViewGroup llLogin = (ViewGroup) findViewById(R.id.llLogin);
		llLogin.setVisibility(View.VISIBLE);

		final EditText txtUsername = (EditText) findViewById(R.id.txtUsername);
		txtUsername.requestFocus();

		this.btnContinue.setOnClickListener(this.btnSwContinueListener);
	}

	private final OnClickListener btnSwContinueListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			doSwFetchConfig();
		}
	};

	protected void doSwFetchConfig () {
		Toast.makeText(this, "TODO: fetch SW config.", Toast.LENGTH_SHORT).show();
	}

	private static void alertAndClose (final Activity activity, final Exception e) {
		alertAndClose(activity, "Error: " + e.toString());
	}

	private static void alertAndClose (final Activity activity, final String msg) {
		new AlertDialog.Builder(activity)
				.setMessage(msg)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick (final DialogInterface dialog, final int which) {
						activity.finish();
					}
				})
				.show();
	}

	private enum SetupActionType {
		SWIMPORT("swimport"),
		WRITETEMPLATE("writetemplate"),
		USETEMPLATE("usetemplate");

		private final String id;

		private SetupActionType (final String id) {
			this.id = id;
		}

		public String getId () {
			return this.id;
		}

		public SetupAction toSetupAction (final Context context) {
			return new SetupAction(this, context);
		}

	}

	private static class SetupAction {

		private final SetupActionType type;
		private final String title;
		private final String description;

		public SetupAction (final SetupActionType type, final Context context) {
			this.type = type;
			final Resources res = context.getResources();
			this.title = res.getString(res.getIdentifier("setup_act_" + type.getId(), "string", context.getPackageName()));
			this.description = res.getString(res.getIdentifier("setup_des_" + type.getId(), "string", context.getPackageName()));
		}

		public SetupActionType getType () {
			return this.type;
		}

		public String getDescription () {
			return this.description;
		}

		@Override
		public String toString () {
			return this.title;
		}

	}

}
