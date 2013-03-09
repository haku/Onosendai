package com.vaguehope.onosendai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

@Ignore
public class ConfigTest {

	private Config undertest;

	@Before
	public void before () throws Exception {
		this.undertest = new Config();
	}

	@Test
	public void itDoesSomething () throws Exception {
		System.err.println("desu");
	}

	private String fixture (final String path) throws IOException {
		StringBuilder s = new StringBuilder();
		BufferedReader r = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(path)));
		try {
			String line;
			while((line = r.readLine()) != null) {
				s.append(line).append("\n");
			}
			return s.toString();
		}
		finally {
			r.close();
		}
	}

}
