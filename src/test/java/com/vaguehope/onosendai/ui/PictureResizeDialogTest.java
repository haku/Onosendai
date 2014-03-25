package com.vaguehope.onosendai.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowLog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.util.IoHelper;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowBitmapRegionDecoder.class })
public class PictureResizeDialogTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	private AtomicInteger okClick;
	private AtomicInteger cancelClick;
	private Activity activity;
	private Uri attachment;

	private AlertDialog alert;
	private ShadowAlertDialog shadowAlert;

	@Before
	public void before () throws Exception {
		ShadowLog.stream = System.out;

		this.okClick = spy(new AtomicInteger(0));
		this.cancelClick = spy(new AtomicInteger(0));
		this.activity = Robolectric.buildActivity(Activity.class).create().get();

		final File picFile = this.tmp.newFile();
		IoHelper.copy(new File("./res/drawable-hdpi/ic_hosaka_meji.png"), picFile); // Just something to test with.
		this.attachment = Uri.fromFile(picFile);

		final PictureResizeDialog dlg = new PictureResizeDialog(this.activity, this.attachment);
		dlg.init();
		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this.activity);
		dlgBuilder.setTitle(dlg.getUiTitle());
		dlgBuilder.setView(dlg.getRootView());
		dlgBuilder.setPositiveButton(android.R.string.ok, new android.content.DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				PictureResizeDialogTest.this.okClick.incrementAndGet();
				dialog.dismiss();
				dlg.recycle();
			}
		});
		dlgBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int whichButton) {
				PictureResizeDialogTest.this.cancelClick.incrementAndGet();
				dialog.cancel();
				dlg.recycle();
			}
		});
		this.alert = dlgBuilder.create();
		this.shadowAlert = Robolectric.shadowOf(this.alert);
	}

	@Test
	public void itCanBeCancelled () throws Exception {
		this.alert.show();

		assertTrue(this.shadowAlert.isCancelable());
		assertFalse(this.shadowAlert.hasBeenDismissed());

		this.alert.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
		assertTrue(this.shadowAlert.hasBeenDismissed());

		assertEquals(0, this.okClick.get());
		assertEquals(1, this.cancelClick.get());

		final ShadowActivity shadowActivity = Robolectric.shadowOf(this.activity);
		assertNull(shadowActivity.getNextStartedActivity());
	}

	@Test
	public void itGeneratesLivePreview () throws Exception {
		this.alert.show();

		final TextView txtSummary = (TextView) this.shadowAlert.getView().findViewById(R.id.txtSummary);

		final String expectedStatus = "100 x 100 (3.4 KB) --> 100 x 100 (78 B)";
		int n = 0;
		while (true) {
			if (expectedStatus.equals(txtSummary.getText().toString())) break;
			if (n > 5) fail("Expected '" + txtSummary.getText() + "' to be '" + expectedStatus + "' after " + n + " seconds.");
			n++;
			Thread.sleep(1000L);
		}

		// TODO assert img in UI is set.
	}

}
