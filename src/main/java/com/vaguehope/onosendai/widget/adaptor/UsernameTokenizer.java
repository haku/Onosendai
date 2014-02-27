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
		int i = cursor;
		while (i > 0 && text.charAt(i - 1) != '@') {
			i--;
		}
		if (i < 1 || text.charAt(i - 1) != '@') return cursor;
		return i;
	}

	@Override
	public int findTokenEnd (final CharSequence text, final int cursor) {
		int i = cursor;
		final int len = text.length();
		while (i < len) {
			if (text.charAt(i) == ' ') return i;
			i++;
		}
		return len;
	}

}