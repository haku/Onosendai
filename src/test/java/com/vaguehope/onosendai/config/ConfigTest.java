package com.vaguehope.onosendai.config;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Map;

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
	public void itReadsAccounts () throws Exception {
		Config conf = new Config();

		Map<String, Account> as = conf.getAccounts();
		assertEquals(1, as.size());

		Account a = as.get("t0");
		assertEquals(AccountProvider.TWITTER, a.provider);
		assertEquals("?ckey?", a.consumerKey);
		assertEquals("?csecret?", a.consumerSecret);
		assertEquals("?atoken?", a.accessToken);
		assertEquals("?asecret?", a.accessSecret);
	}

	@Test
	public void itReadsColumns () throws Exception {
		Config conf = new Config();

		Map<Integer, Column> cs = conf.getColumns();
		assertEquals(2, cs.size());

		Column c0 = cs.get(Integer.valueOf(0));
		assertEquals(0, c0.id);
		assertEquals("main", c0.title);
		assertEquals("t0", c0.accountId);
		assertEquals("timeline", c0.resource);
		assertEquals("15min", c0.refresh);

		Column c1 = cs.get(Integer.valueOf(1));
		assertEquals(1, c1.id);
		assertEquals("my list", c1.title);
		assertEquals("t0", c1.accountId);
		assertEquals("lists/mylist", c1.resource);
		assertEquals("15min", c1.refresh);
	}

	private String fixture (final String path) throws IOException {
		return IOUtils.toString(getClass().getResourceAsStream(path));
	}

}
