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
		new AlertDialog.Builder(context)
				.setMessage(msg)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick (final DialogInterface dialog, final int which) {
						dialog.dismiss();
					}
				})
				.show();
	}

	public static void alertAndClose (final Activity activity, final Exception e) {
		alertAndClose(activity, "Error: " + e.toString());
	}

	public static void alertAndClose (final Activity activity, final String msg) {
		new AlertDialog.Builder(activity)
				.setMessage(msg)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick (final DialogInterface dialog, final int which) {
						activity.finish();
						dialog.dismiss();
					}
				})
				.show();
	}

	public static final DialogInterface.OnClickListener DLG_CANCEL_CLICK_LISTENER = new DialogInterface.OnClickListener() {
		@Override
		public void onClick (final DialogInterface dialog, final int whichButton) {
			dialog.cancel();
		}
	};

}
