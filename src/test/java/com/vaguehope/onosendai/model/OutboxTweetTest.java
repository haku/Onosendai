package com.vaguehope.onosendai.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.HashSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.OutboxTweet.OutboxAction;
import com.vaguehope.onosendai.model.OutboxTweet.OutboxTweetStatus;
import com.vaguehope.onosendai.provider.ServiceRef;

@RunWith(MockitoJUnitRunner.class)
public class OutboxTweetTest {

	@Mock private Account account;

	@Test
	public void itParsesOutboxActionCodeCorrectly () throws Exception {
		for (final OutboxAction a : OutboxAction.values()) {
			assertSame(a, OutboxAction.parseCode(a.getCode()));
		}
	}

	@Test
	public void itParsesOutboxTweetStatusCodeCorrectly () throws Exception {
		for (final OutboxTweetStatus a : OutboxTweetStatus.values()) {
			assertSame(a, OutboxTweetStatus.parseCode(a.getCode()));
		}
	}

	@Test
	public void itsOkWithEmptyServiceSet () throws Exception {
		final OutboxTweet ot = new OutboxTweet(null, new Account("a", null, null, null, null, null, null),
				new HashSet<ServiceRef>(), null, null, null);
		assertThat(ot.getSvcMetasList(), is(empty()));
	}

	@Test
	public void itsOkWithNullServiceString () throws Exception {
		final OutboxTweet ot = new OutboxTweet(0L, null, null, null, null, null, null, null, null, null, null, null);
		assertThat(ot.getSvcMetasList(), is(empty()));
	}

	@Test
	public void itsOkWithEmptyServiceString () throws Exception {
		final OutboxTweet ot = new OutboxTweet(0L, null, null, "", null, null, null, null, null, null, null, null);
		assertThat(ot.getSvcMetasList(), is(empty()));
	}

	@Test
	public void itConstructsWithAction () throws Exception {
		final OutboxTweet ot = new OutboxTweet(OutboxAction.RT, this.account, null, null, null, null);
		assertEquals(OutboxAction.RT, ot.getAction());
	}

	@Test
	public void itCreatesTempSid () throws Exception {
		final OutboxTweet ot = new OutboxTweet(Long.MAX_VALUE, null, null, "", null, null, null, null, null, null, null, null);
		assertEquals("outbox:" + Long.MAX_VALUE, ot.getTempSid());
	}

	@Test
	public void itParsesTempSid () throws Exception {
		assertEquals(Long.MAX_VALUE, OutboxTweet.uidFromTempSid("outbox:" + Long.MAX_VALUE));
	}

}
