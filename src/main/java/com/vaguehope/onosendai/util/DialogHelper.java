package com.vaguehope.onosendai.util;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

public final class DialogHelper {

	public interface Listener<T> {
		public void onAnswer (T answer);
	}

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
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
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
		dlgBld.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				dialog.dismiss();
				onYes.run();
			}
		});
		dlgBld.setNegativeButton(android.R.string.no, DLG_CANCEL_CLICK_LISTENER);
		dlgBld.show();
	}

	public static void askString(final Context context, final String msg, final Listener<String> onString) {
		final AlertDialog.Builder dlgBld = new AlertDialog.Builder(context);
		dlgBld.setMessage(msg);
		final EditText editText = new EditText(context);
		editText.setSelectAllOnFocus(true);
		editText.setSingleLine();
		editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
		dlgBld.setView(editText);
		dlgBld.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				dialog.dismiss();
				onString.onAnswer(editText.getText().toString().trim());
			}
		});
		dlgBld.setNegativeButton(android.R.string.cancel, DLG_CANCEL_CLICK_LISTENER);
		dlgBld.show();
	}

	public static void askItemFromList(final Context context, final String title, final List<String> list, final Listener<String> onItem) {
		final AlertDialog.Builder bld = new AlertDialog.Builder(context);
		bld.setTitle(title);
		bld.setNegativeButton(android.R.string.cancel, DialogHelper.DLG_CANCEL_CLICK_LISTENER);
		bld.setItems(list.toArray(new String[]{}), new SimpleAnswerListener<String>(list, onItem));
		bld.show();
	}

	public static final DialogInterface.OnClickListener DLG_CANCEL_CLICK_LISTENER = new DialogInterface.OnClickListener() {
		@Override
		public void onClick (final DialogInterface dialog, final int whichButton) {
			dialog.cancel();
		}
	};

	public static class SimpleAnswerListener<T> implements DialogInterface.OnClickListener {

		private final List<T> items;
		private final Listener<T> onAnswer;

		public SimpleAnswerListener (final List<T> items, final Listener<T> onAnswer) {
			this.items = items;
			this.onAnswer = onAnswer;
		}

		@Override
		public void onClick (final DialogInterface dialog, final int which) {
			dialog.dismiss();
			this.onAnswer.onAnswer(this.items.get(which));
		}

	}

}
