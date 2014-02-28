package com.vaguehope.onosendai.widget.adaptor;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.MultiAutoCompleteTextView.Tokenizer;

/**
 * https://stackoverflow.com/questions/12691679
 */
public class UsernameTokenizer implements Tokenizer {

	public UsernameTokenizer () {}

	@Override
	public CharSequence terminateToken (final CharSequence text) {
		int i = text.length();
		while (i > 0 && text.charAt(i - 1) == ' ') {
			i--;
		}
		if (i > 0 && text.charAt(i - 1) == ' ') return text;
		if (text instanceof Spanned) {
			final SpannableString sp = new SpannableString(text + " ");
			TextUtils.copySpansFrom((Spanned) text, 0, text.length(), Object.class, sp, 0);
			return sp;
		}
		return text + " ";
	}

	@Override
	public int findTokenStart (final CharSequence text, final int cursor) {
		if (cursor == 0 || text.length() < 1 || isStartChar(text.charAt(cursor - 1))) return cursor;
		int i = cursor;
		while (i > 0 && !isStartChar(text.charAt(i - 1))) {
			i--;
		}
		if (i < 1 || !isStartChar(text.charAt(i - 1))) return cursor;
		return i - 1;
	}

	@Override
	public int findTokenEnd (final CharSequence text, final int cursor) {
		int i = cursor;
		final int len = text.length();
		while (i < len) {
			if (isEndChar(text.charAt(i))) return i;
			i++;
		}
		return len;
	}

	private static boolean isStartChar(final char c) {
		return c == '@' || c == '#';
	}

	private static boolean isEndChar(final char c) {
		return c == '@' || c == ' ';
	}

}