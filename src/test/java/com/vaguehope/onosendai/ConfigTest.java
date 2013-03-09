package com.vaguehope.onosendai;

import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import android.os.Environment;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Environment.class })
public class ConfigTest {

	@Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Before
	public void before () throws Exception {
		PowerMockito.mockStatic(Environment.class);
		when(Environment.getExternalStorageDirectory()).thenReturn(this.temporaryFolder.getRoot());
		String conf = fixture("/deck.conf");
		File f = new File(this.temporaryFolder.getRoot(), "deck.conf");
		FileUtils.write(f, conf);
	}

	@Test
	public void itReadsConfig () throws Exception {
		Config conf = new Config();
	}

	private String fixture (final String path) throws IOException {
		return IOUtils.toString(getClass().getResourceAsStream(path));
	}

}
