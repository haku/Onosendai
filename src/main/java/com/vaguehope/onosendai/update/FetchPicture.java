package com.vaguehope.onosendai.update;

import java.util.concurrent.Callable;

import com.vaguehope.onosendai.images.HybridBitmapCache;
import com.vaguehope.onosendai.util.HttpHelper;
import com.vaguehope.onosendai.util.HttpHelper.Method;

public class FetchPicture implements Callable<Void> {

	private final HybridBitmapCache hybridBitmapCache;
	private final String mediaUrl;

	public FetchPicture (final HybridBitmapCache hybridBitmapCache, final String mediaUrl) {
		if (hybridBitmapCache == null) throw new IllegalArgumentException("hybridBitmapCache can not be null.");
		if (mediaUrl == null) throw new IllegalArgumentException("mediaUrl can not be null.");
		this.hybridBitmapCache = hybridBitmapCache;
		this.mediaUrl = mediaUrl;
	}

	@Override
	public Void call () throws Exception {
		if (this.hybridBitmapCache.getCachedFile(this.mediaUrl) == null) {
			HttpHelper.fetch(Method.GET, this.mediaUrl, this.hybridBitmapCache.fromHttp(this.mediaUrl));
		}
		return null;
	}

}
