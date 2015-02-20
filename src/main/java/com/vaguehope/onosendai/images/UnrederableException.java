package com.vaguehope.onosendai.images;

import java.io.File;
import java.io.IOException;

public class UnrederableException extends IOException {

	private static final long serialVersionUID = -5115494476279391567L;

	public UnrederableException (final File file) {
		super("Unrederable: " + file.getAbsolutePath()); //ES
	}

}
