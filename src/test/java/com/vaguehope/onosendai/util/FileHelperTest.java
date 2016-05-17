package com.vaguehope.onosendai.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
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

	@Test
	public void itMakesSafeFileNames () throws Exception {
		assertEquals("file.txt", FileHelper.makeSafeName("file.txt"));
		assertEquals("fileA1.-.txt", FileHelper.makeSafeName("fileA1.-.txt"));
		assertEquals("file_._.txt", FileHelper.makeSafeName("file+*.?.txt"));
	}

	@Test
	public void itFindsNameFromPath () throws Exception {
		assertEquals("example.com_file.txt", FileHelper.nameFromPath("http://example.com/file.txt"));
		assertEquals("example.com_path_file.txt", FileHelper.nameFromPath("http://example.com/path/file.txt"));
		assertEquals("example.com_path_file", FileHelper.nameFromPath("http://example.com/path/file"));
		assertEquals("example.com_1", FileHelper.nameFromPath("http://example.com/1"));
		assertEquals("example.com_12345_1", FileHelper.nameFromPath("http://example.com:12345/1"));
		assertEquals("example.com_1_large", FileHelper.nameFromPath("http://example.com/1:large"));
		assertEquals("example.com_12345_123_small", FileHelper.nameFromPath("http://example.com:12345/123:small"));
		assertEquals("instagram.com_p_BFdoIydtZzU_media_size_m", FileHelper.nameFromPath("https://instagram.com/p/BFdoIydtZzU/media/?size=m"));
		assertEquals("1", FileHelper.nameFromPath("/1"));
		assertEquals("a_1", FileHelper.nameFromPath("a/1"));
		assertEquals("1", FileHelper.nameFromPath("1"));
		assertEquals("1", FileHelper.nameFromPath("1/"));
		assertEquals("a", FileHelper.nameFromPath("//a"));
		assertEquals("a", FileHelper.nameFromPath("///a"));
		assertEquals("example.com", FileHelper.nameFromPath("http://example.com"));
		assertEquals("example.com", FileHelper.nameFromPath("http://example.com/"));
		assertEquals(null, FileHelper.nameFromPath("//"));
		assertEquals(null, FileHelper.nameFromPath("/"));
		assertEquals(null, FileHelper.nameFromPath(""));
		assertEquals(null, FileHelper.nameFromPath(null));
	}

}
