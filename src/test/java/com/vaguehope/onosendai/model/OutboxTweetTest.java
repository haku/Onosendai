package com.vaguehope.onosendai.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.HashSet;

import org.junit.Test;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.provider.ServiceRef;

public class OutboxTweetTest {

	@Test
	public void itsOkWithEmptyServiceSet () throws Exception {
		final OutboxTweet ot = new OutboxTweet(new Account("a", null, null, null, null, null, null),
				new HashSet<ServiceRef>(), null, null, null);
		assertThat(ot.getSvcMetasList(), is(empty()));
	}

	@Test
	public void itsOkWithNullServiceString () throws Exception {
		final OutboxTweet ot = new OutboxTweet(null, null, null, null, null, null, null, null, null);
		assertThat(ot.getSvcMetasList(), is(empty()));
	}

	@Test
	public void itsOkWithEmptyServiceString () throws Exception {
		final OutboxTweet ot = new OutboxTweet(null, null, "", null, null, null, null, null, null);
		assertThat(ot.getSvcMetasList(), is(empty()));
	}

}
