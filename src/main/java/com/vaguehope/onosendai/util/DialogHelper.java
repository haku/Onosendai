package com.vaguehope.onosendai.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.text.InputType;
import android.view.WindowManager.BadTokenException;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.util.CollectionHelper.Function;

public final class DialogHelper {

	public interface Listener<T> {
		void onAnswer (T answer);
	}

	public interface Question<T> {
		boolean ask (T arg);
	}

	private DialogHelper () {
		throw new AssertionError();
	}

	public static void alert (final Context context, final Throwable t) {
		alert(context, context.getString(R.string.dialog_general_error), t);
	}

	public static void alert (final Context context, final String msg, final Throwable t) {
		alert(context, msg + "\n" + ExcpetionHelper.causeTrace(t));
	}

	public static void alertIfPossible (final Context context, final String msg, final Exception e) {
		try {
			alert(context, msg, e);
		}
		catch (BadTokenException ignored) {/* If the UI context is no longer valid. */} // NOSONAR exception intentionally ignored.
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
		alertAndClose(activity, "Error:", e);
	}

	public static void alertAndClose (final Activity activity, final String msg, final Exception e) {
		alertAndClose(activity, msg + "\n" + ExcpetionHelper.causeTrace(e));
	}

	public static void alertAndClose (final Activity activity, final String msg) {
		alertAndRun(activity, msg, new Runnable() {
			@Override
			public void run () {
				activity.finish();
			}
		});
	}

	public static void askYesNo (final Context context, final String msg, final Runnable onYes) {
		askYesNo(context, msg,
				context.getString(R.string.dialog_general_btn_yes),
				context.getString(R.string.dialog_general_btn_no),
				onYes);
	}

	public static void askYesNo (final Context context, final String msg, final String yesLbl, final String noLbl, final Runnable onYes) {
		final AlertDialog.Builder dlgBld = new AlertDialog.Builder(context);
		dlgBld.setMessage(msg);
		dlgBld.setPositiveButton(yesLbl, new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				dialog.dismiss();
				onYes.run();
			}
		});
		dlgBld.setNegativeButton(noLbl, DLG_CANCEL_CLICK_LISTENER);
		dlgBld.show();
	}

	public static void askString (final Context context, final String msg, final Listener<String> onString) {
		askString(context, msg, null, false, true, onString);
	}

	// FIXME tidy this method signature.
	public static void askString (final Context context, final String msg,
			final String oldValue, final boolean multiLine, final boolean spellCheck,
			final Listener<String> onString) {
		final AlertDialog.Builder dlgBld = new AlertDialog.Builder(context);
		dlgBld.setMessage(msg);
		final EditText editText = new EditText(context);
		editText.setSelectAllOnFocus(true);
		if (oldValue != null) editText.setText(oldValue);
		if (!multiLine) editText.setSingleLine();
		if (!spellCheck) editText.setInputType(editText.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
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

	public static void askStringItem (final Context context, final String title, final List<String> list, final Listener<String> onItem) {
		askItem(context, title, list, list.toArray(new String[] {}), onItem);
	}

	public static <T extends Titleable> void askItem (final Context context, final String title, final T[] arr, final Listener<T> onItem) {
		askItem(context, title, Arrays.asList(arr), onItem);
	}

	public static <T extends Titleable> void askItem (final Context context, final String title, final List<T> list, final Listener<T> onItem) {
		final List<String> titles = titlesList(list);
		askItem(context, title, list, titles.toArray(new String[] {}), onItem);
	}

	public static <T> void askItem (final Context context, final String title, final T[] arr, final Function<T, String> titler, final Listener<T> onItem) {
		askItem(context, title, Arrays.asList(arr), titler, onItem);
	}

	public static <T> void askItem (final Context context, final String title, final List<T> list, final Function<T, String> titler, final Listener<T> onItem) {
		final List<String> titles = titlesList(list, titler);
		askItem(context, title, list, titles.toArray(new String[] {}), onItem);
	}

	private static <T> void askItem (final Context context, final String title, final List<T> list, final String[] labels, final Listener<T> onItem) {
		final AlertDialog.Builder bld = new AlertDialog.Builder(context);
		bld.setTitle(title);
		bld.setNegativeButton(android.R.string.cancel, DialogHelper.DLG_CANCEL_CLICK_LISTENER);
		bld.setItems(labels, new SimpleAnswerListener<T>(list, onItem));
		bld.show();
	}

	public static <T extends Titleable> void askItems (final Context context, final String title, final List<T> list, final Listener<Set<T>> onItem) {
		askItems(context, title, list, null, onItem);
	}

	public static <T extends Titleable> void askItems (final Context context, final String title, final List<T> list, final Question<T> isChecked, final Listener<Set<T>> onItem) {
		final List<String> titles = new ArrayList<String>();
		for (final T item : list) {
			titles.add(item.getUiTitle());
		}

		boolean[] arrChecked = null;
		if (isChecked != null) {
			arrChecked = new boolean[list.size()];
			for (int i = 0; i < list.size(); i++) {
				arrChecked[i] = isChecked.ask(list.get(i));
			}
		}

		askItems(context, title, list, titles.toArray(new String[] {}), arrChecked, onItem);
	}

	private static <T> void askItems (final Context context, final String title, final List<T> list, final String[] labels, final boolean[] checked, final Listener<Set<T>> onItems) {
		if (labels.length != list.size()) throw new IllegalArgumentException("List and titles array must be same length.");
		if (checked != null && checked.length != list.size()) throw new IllegalArgumentException("List and checed array must be same length.");

		final AlertDialog.Builder bld = new AlertDialog.Builder(context);
		bld.setTitle(title);
		bld.setNegativeButton(android.R.string.cancel, DialogHelper.DLG_CANCEL_CLICK_LISTENER);
		final Set<T> selectedItems = new HashSet<T>();
		if (checked != null) {
			for (int i = 0; i < list.size(); i++) {
				if (checked[i]) selectedItems.add(list.get(i));
			}
		}
		bld.setMultiChoiceItems(labels, checked, new OnMultiChoiceClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which, final boolean isChecked) {
				final T item = list.get(which);
				if (isChecked) {
					selectedItems.add(item);
				}
				else {
					selectedItems.remove(item);
				}
			}
		});
		bld.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				dialog.dismiss();
				onItems.onAnswer(selectedItems);
			}
		});
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

	private static <T extends Titleable> List<String> titlesList (final List<T> list) {
		final List<String> titles = new ArrayList<String>();
		for (final T item : list) {
			titles.add(item.getUiTitle());
		}
		return titles;
	}

	private static <T> List<String> titlesList (final List<T> list, final Function<T, String> titler) {
		final List<String> titles = new ArrayList<String>();
		for (final T item : list) {
			titles.add(titler.exec(item));
		}
		return titles;
	}

}
