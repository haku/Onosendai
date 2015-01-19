package com.vaguehope.onosendai.images;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowMimeTypeMap;

import android.net.Uri;
import android.webkit.MimeTypeMap;

import com.vaguehope.onosendai.util.IoHelper;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { MyShadowBitmapFactory.class})
public class CachedImageFileProviderTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void itAddsExtension () throws Exception {
		final ShadowMimeTypeMap shadowMimeTypeMap = Robolectric.shadowOf(MimeTypeMap.getSingleton());
		shadowMimeTypeMap.addExtensionMimeTypMapping("png", "image/png");

		final File picFile = this.tmp.newFile();
		IoHelper.copy(new File("./res/drawable-hdpi/ic_hosaka_meji.png"), picFile); // Just something to test with.
		final List<File> actual = CachedImageFileProvider.addFileExtensions(Collections.singletonList(picFile));
		assertThat(actual, hasItem(new File(picFile.getAbsolutePath() + ".png")));
	}

	@Test
	public void itRemovesExtension () throws Exception {
		final Uri input = Uri.fromFile(new File("/some/path/file.jpg"));
		final Uri output = Uri.fromFile(new File("/some/path/file"));
		assertEquals(output, CachedImageFileProvider.removeExtension(input));
	}

	@Test
	public void itMakesIdFromUri () throws Exception {
		final Uri uri = Uri.parse("content://com.vaguehope.onosendai.fileprovider/images/c8633fb04879b0ca35f662459a88e03b");
		final long id = CachedImageFileProvider.tryReadId(uri);
		assertEquals(69350895355748411L, id);
	}

}
