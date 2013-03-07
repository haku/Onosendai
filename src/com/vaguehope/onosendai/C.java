package com.vaguehope.onosendai;

import java.io.File;

import android.os.Environment;

public interface C {

	String TAG = "onosendai";

	File CONFIG_FILE = new File(Environment.getExternalStorageDirectory().getPath(), "deck.conf");

	String DATA_TW_MAX_AGE_DAYS = "-7 days";

}
