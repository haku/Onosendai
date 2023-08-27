package com.vaguehope.onosendai.update;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import android.app.NotificationManager;
import android.content.Context;

@RunWith(RobolectricTestRunner.class)
public class BatteryNotifyTest {

	@Mock private Context context;
	@Mock private NotificationManager notificationManager;
	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	@Before
	public void before () throws Exception {
		MockitoAnnotations.initMocks(this);
		when(this.context.getCacheDir()).thenReturn(this.tmp.getRoot());
		when(this.context.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(this.notificationManager);
	}

	@Test
	public void itMarksOverride () throws Exception {
		assertFalse(BatteryNotify.isOverrideEnabled(this.context));
		assertFalse(BatteryNotify.isOverrideEnabled(this.context));
		verify(this.context, times(1)).getCacheDir();

		BatteryNotify.enableOverride(this.context);
		verify(this.context, times(2)).getCacheDir();

		assertTrue(BatteryNotify.isOverrideEnabled(this.context));
		assertTrue(BatteryNotify.isOverrideEnabled(this.context));
		verify(this.context, times(3)).getCacheDir();
	}

	@Test
	public void itMarksAndResetsNotification () throws Exception {
		assertTrue(BatteryNotify.shouldNotifyNotUpdating(this.context));
		assertFalse(BatteryNotify.shouldNotifyNotUpdating(this.context));
		assertFalse(BatteryNotify.shouldNotifyNotUpdating(this.context));
		verify(this.context, times(1)).getCacheDir();

		BatteryNotify.clearNotUpdating(this.context);
		verify(this.context, times(2)).getCacheDir();

		assertTrue(BatteryNotify.shouldNotifyNotUpdating(this.context));
		assertFalse(BatteryNotify.shouldNotifyNotUpdating(this.context));
		assertFalse(BatteryNotify.shouldNotifyNotUpdating(this.context));
		verify(this.context, times(3)).getCacheDir();
	}

}
