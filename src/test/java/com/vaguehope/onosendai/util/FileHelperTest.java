package com.vaguehope.onosendai.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileHelperTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void itCreatesNewFile () throws Exception {
		assertThat(FileHelper.newFileInDir(this.tmp.getRoot(), "test file-1.txt").getName(),
				equalTo("test_file-1.txt"));
	}

	@Test
	public void itAddsRandomNumberIfFileAlreadyExists () throws Exception {
		this.tmp.newFile("test_file-1.txt");
		final String name = FileHelper.newFileInDir(this.tmp.getRoot(), "test file-1.txt").getName();
		if (!name.matches("^test_file-1.[0-9]+.txt$")) fail("Does not match pattern: " + name);
	}

}
