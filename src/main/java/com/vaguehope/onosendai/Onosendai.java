package com.vaguehope.onosendai;

import java.io.IOException;
import java.util.Locale;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.res.Configuration;
import android.util.Log;

import com.vaguehope.onosendai.ui.pref.UiPrefFragment;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.LocaleHelper;
import com.vaguehope.onosendai.util.LogWrapper;

@ReportsCrashes(formKey = "" /* not used */,
		mailTo = "reports@onosendai.mobi",
		mode = ReportingInteractionMode.DIALOG,
		resDialogText = R.string.crash_dialog_text,
		resDialogIcon = R.drawable.exclamation_red,
		resDialogTitle = R.string.crash_dialog_title, // optional. default is your application name
		resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, // optional. when defined, adds a user text field input with this text resource as a label
		resDialogOkToast = R.string.crash_dialog_ok_toast,
		customReportContent = {
				ReportField.USER_COMMENT,
				ReportField.ANDROID_VERSION,
				ReportField.APP_VERSION_NAME,
				ReportField.BRAND,
				ReportField.PHONE_MODEL,
				ReportField.BUILD,
				ReportField.PRODUCT,
				ReportField.AVAILABLE_MEM_SIZE,
				ReportField.INSTALLATION_ID,
				ReportField.USER_APP_START_DATE,
				ReportField.STACK_TRACE,
				ReportField.THREAD_DETAILS,
				ReportField.CUSTOM_DATA,
				ReportField.LOGCAT
		})
public class Onosendai extends Application {

	private static final LogWrapper LOG = new LogWrapper("APP");

	@Override
	public void onCreate () {
		super.onCreate();
		ACRA.init(this);
		addBuildNumberToCrashReport();
		LOG.i("RT.maxMemory=%s", Runtime.getRuntime().maxMemory());
		loadAndSetLocale(null);
	}

	private void addBuildNumberToCrashReport () {
		try {
			final String buildNumber = IoHelper.toString(getClass().getResourceAsStream("/build_number"));
			ACRA.getErrorReporter().putCustomData("BUILD_NUMBER", buildNumber);
		}
		catch (final IOException e) {
			Log.w(C.TAG, "Failed to read BUILD_NUMBER: " + e.toString());
		}
	}

	@Override
	public void onConfigurationChanged (final Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		LOG.d("config: %s", newConfig);
		loadAndSetLocale(newConfig);
	}

	private void loadAndSetLocale (final Configuration roCfg) {
		final Locale locale = UiPrefFragment.readLocale(this);
		LOG.i("locale=%s", locale);
		LocaleHelper.setLocale(this, roCfg, locale);
	}

}
