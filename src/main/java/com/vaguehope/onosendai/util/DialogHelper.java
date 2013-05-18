package com.vaguehope.onosendai.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public final class DialogHelper {

	private DialogHelper () {
		throw new AssertionError();
	}

	public static void alert (final Context context, final Exception e) {
		alert(context, "Error: " + e.toString());
	}

	public static void alert (final Context context, final String msg, final Exception e) {
		alert(context, msg + e.toString());
	}

	public static void alert (final Context context, final String msg) {
		alertAndRun(context, msg, null);
	}

	public static void alertAndRun (final Context context, final String msg, final Runnable run) {
		new AlertDialog.Builder(context)
		.setMessage(msg)
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				dialog.dismiss();
				if (run != null) run.run();
			}
		})
		.show();
	}

	public static void alertAndClose (final Activity activity, final Exception e) {
		alertAndClose(activity, "Error: " + e.toString());
	}

	public static void alertAndClose (final Activity activity, final String msg) {
		alertAndRun(activity, msg, new Runnable() {
			@Override
			public void run () {
				activity.finish();
			}
		});
	}

	public static void askYesNo(final Context context, final String msg, final Runnable onYes) {
		final AlertDialog.Builder dlgBld = new AlertDialog.Builder(context);
		dlgBld.setMessage(msg);
		dlgBld.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				onYes.run();
			}
		});
		dlgBld.setNegativeButton("No", DLG_CANCEL_CLICK_LISTENER);
		dlgBld.show();
	}

	public static final DialogInterface.OnClickListener DLG_CANCEL_CLICK_LISTENER = new DialogInterface.OnClickListener() {
		@Override
		public void onClick (final DialogInterface dialog, final int whichButton) {
			dialog.cancel();
		}
	};

}
