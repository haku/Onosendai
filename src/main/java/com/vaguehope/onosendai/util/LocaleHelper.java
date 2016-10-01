package com.vaguehope.onosendai.util;

import java.util.Locale;

import android.annotation.TargetApi;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

public class LocaleHelper {

	private static final Locale BASE_LOCALE = Locale.getDefault();

	private LocaleHelper () {
		throw new AssertionError();
	}

	public static void setLocale (final ContextWrapper contextWrapper, final Locale locale) {
		setLocale(contextWrapper, null, locale);
	}

	public static Locale setLocale (final ContextWrapper contextWrapper, final Configuration roCfg, final Locale reqLocale) {
		final Locale newLocale = reqLocale != null ? reqLocale : BASE_LOCALE;
		Locale.setDefault(newLocale);
		final Resources resources = contextWrapper.getBaseContext().getResources();
		final Configuration config = roCfg != null ? new Configuration(roCfg) : resources.getConfiguration();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			setLocaleJbmr1(newLocale, config);
		}
		else {
			config.locale = newLocale;
		}
		resources.updateConfiguration(config, resources.getDisplayMetrics());
		return newLocale;
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	private static void setLocaleJbmr1 (final Locale locale, final Configuration config) {
		config.setLocale(locale);
	}

}
