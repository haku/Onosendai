package com.vaguehope.onosendai.ui;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.io.InputStream;

import org.robolectric.Robolectric;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;

/**
 * http://robolectric.blogspot.co.uk/2011/01/how-to-create-your-own-shadow-classes.html
 * http://robolectric.blogspot.co.uk/2013/05/configuring-robolectric-20.html
 */
@Implements(BitmapRegionDecoder.class)
public class ShadowBitmapRegionDecoder {

	@RealObject private BitmapRegionDecoder bitmapRegionDecoder;

	private BitmapRegionDecoder spy;
	private InputStream inputstream;
	private boolean shareable;

	private void setSpy (final BitmapRegionDecoder spy) {
		this.spy = spy;
	}

	private void setInputStream (final InputStream is) {
		this.inputstream = is;
	}

	private void setSharable (final boolean shareable) {
		this.shareable = shareable;
	}

	public BitmapRegionDecoder getSpy () {
		return this.spy;
	}

	public InputStream getIs () {
		return this.inputstream;
	}

	public boolean isShareable () {
		return this.shareable;
	}

	@Implementation
	public int getWidth() {
		return 72; // FIXME do not hard code.
	}

	@Implementation
	public int getHeight() {
		return 72; // FIXME do not hard code.
	}

	@Implementation
	public Bitmap decodeRegion(final Rect rect, final BitmapFactory.Options options) {
		// TODO mock out metadata based on parameters?
		return mock(Bitmap.class);
	}

	@Implementation
	public void recycle() {
		// TODO assert this was called?
	}

	/**
	 * Mock impl.
	 * @throws IOException
	 */
	@Implementation
	public static BitmapRegionDecoder newInstance (final InputStream is, final boolean isShareable) throws IOException {
		final BitmapRegionDecoder brd = spy(Robolectric.newInstanceOf(BitmapRegionDecoder.class));

		final ShadowBitmapRegionDecoder shadowBrd = Robolectric.shadowOf_(brd);
		shadowBrd.setSpy(brd);
		shadowBrd.setInputStream(is);
		shadowBrd.setSharable(isShareable);

		System.err.println("is=" + is + " isShareable=" + isShareable);
		return brd;
	}

}
