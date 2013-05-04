package com.vaguehope.onosendai.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class SyncMgrTest {

	private SyncMgr undertest;

	@Before
	public void before () throws Exception {
		this.undertest = new SyncMgr();
	}

	@Test
	public void itReturnsSameSyncPerKey () throws Exception {
		Object a = this.undertest.getSync(new String("absd"));
		Object b = this.undertest.getSync(new String("absd"));
		Object c = this.undertest.getSync(new String("abse"));
		assertThat(b, sameInstance(a));
		assertThat(c, not(equalTo(a)));
	}

	@Test
	public void itRembersAfterFirstReturn () throws Exception {
		Object a = this.undertest.getSync(new String("absd"));
		Object b = this.undertest.getSync(new String("absd"));
		assertThat(b, sameInstance(a));
		this.undertest.returnSync(new String("absd"));
		Object c = this.undertest.getSync(new String("absd"));
		assertThat(c, sameInstance(a));
	}

	@Test
	public void itFogetsAfterLastReturn () throws Exception {
		Object a = this.undertest.getSync(new String("absd"));
		this.undertest.returnSync(new String("absd"));
		Object b = this.undertest.getSync(new String("absd"));
		assertThat(b, not(equalTo(a)));
	}

}
