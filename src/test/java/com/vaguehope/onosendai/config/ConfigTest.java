package com.vaguehope.onosendai.config;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;
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

	private Config undertest;

	@Before
	public void before () throws Exception {
		PowerMockito.mockStatic(Environment.class);
		when(Environment.getExternalStorageDirectory()).thenReturn(this.temporaryFolder.getRoot());
		File f = new File(this.temporaryFolder.getRoot(), "deck.conf");
		FileUtils.write(f, fixture("/deck.conf"));

		this.undertest = Config.getConfig();
	}

	@Test
	public void itReadsAccounts () throws Exception {
		Map<String, Account> as = this.undertest.getAccounts();
		assertEquals(3, as.size());

		Account a = as.get("t0");
		assertEquals(AccountProvider.TWITTER, a.getProvider());
		assertEquals("?ckey?", a.getConsumerKey());
		assertEquals("?csecret?", a.getConsumerSecret());
		assertEquals("?atoken?", a.getAccessToken());
		assertEquals("?asecret?", a.getAccessSecret());

		Account b = as.get("sw0");
		assertEquals(AccountProvider.SUCCESSWHALE, b.getProvider());
		assertEquals(null, b.getConsumerKey());
		assertEquals(null, b.getConsumerSecret());
		assertEquals("?username?", b.getAccessToken());
		assertEquals("?password?", b.getAccessSecret());

		Account c = as.get("b0");
		assertEquals(AccountProvider.BUFFER, c.getProvider());
		assertEquals(null, c.getConsumerKey());
		assertEquals(null, c.getConsumerSecret());
		assertEquals("?accesstoken?", c.getAccessToken());
		assertEquals(null, c.getAccessSecret());
	}

	@Test
	public void itReadsColumns () throws Exception {
		List<Column> cs = this.undertest.getColumns();
		assertEquals(3, cs.size());

		Column c0 = cs.get(0);
		assertEquals(0, c0.getId());
		assertEquals("main", c0.getTitle());
		assertEquals("t0", c0.getAccountId());
		assertEquals("timeline", c0.getResource());
		assertEquals(15, c0.getRefreshIntervalMins());
		assertEquals(false, c0.isNotify());

		Column c1 = cs.get(1);
		assertEquals(1, c1.getId());
		assertEquals("my list", c1.getTitle());
		assertEquals("t0", c1.getAccountId());
		assertEquals("lists/mylist", c1.getResource());
		assertEquals(15, c1.getRefreshIntervalMins());
		assertEquals(true, c1.isNotify());

		Column c2 = cs.get(2);
		assertEquals(2, c2.getId());
		assertEquals("reading list", c2.getTitle());
		assertEquals(null, c2.getAccountId());
		assertEquals("later", c2.getResource());
		assertEquals(0, c2.getRefreshIntervalMins());
		assertEquals(false, c2.isNotify());
	}

	private String fixture (final String path) throws IOException {
		return IOUtils.toString(getClass().getResourceAsStream(path));
	}

}
