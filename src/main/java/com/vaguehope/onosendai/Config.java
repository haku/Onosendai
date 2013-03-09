package com.vaguehope.onosendai;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONTokener;

import com.vaguehope.onosendai.util.FileHelper;

public class Config {

	public Config () throws IOException, JSONException {
		File f = C.CONFIG_FILE;
		String s = FileHelper.fileToString(f);
		Object o = new JSONTokener(s).nextValue();
		System.err.println(o);
	}

}
