package com.vaguehope.onosendai.images;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.vaguehope.onosendai.util.LogWrapper;

public class ImageLoadRequestManager {

	private static final LogWrapper LOG = new LogWrapper("ILRM");

	private final ConcurrentMap<String, List<ImageLoadRequest>> reqs = new ConcurrentHashMap<String, List<ImageLoadRequest>>();

	public void registerRequest (final ImageLoadRequest req) {
		cleanUp(); // FIXME doing this here is a lazy hack.

		List<ImageLoadRequest> list = this.reqs.get(req.getUrl());
		if (list == null) {
			final List<ImageLoadRequest> newList = new ArrayList<ImageLoadRequest>();
			final List<ImageLoadRequest> prevList = this.reqs.putIfAbsent(req.getUrl(), newList);
			list = prevList != null ? prevList : newList;
		}
		synchronized (list) {
			list.add(req);
		}
	}

	public void unregisterRequest (final ImageLoadRequest req) {
		final List<ImageLoadRequest> list = this.reqs.get(req.getUrl());
		if (list == null) return;
		synchronized (list) {
			list.remove(req);
		}
	}

	public void setLoadingProgressIfRequired (final ImageLoadRequest originReq, final String msg) {
		final List<ImageLoadRequest> list = this.reqs.get(originReq.getUrl());
		if (list == null) return;
		synchronized (list) {
			for (final ImageLoadRequest req : list) {
				if (req != originReq) req.setLoadingProgressIfRequired(msg);
			}
		}
	}

	public void setFetchingProgressIfRequired (final ImageLoadRequest originReq, final Integer progress, final Integer total) {
		final List<ImageLoadRequest> list = this.reqs.get(originReq.getUrl());
		if (list == null) return;
		synchronized (list) {
			for (final ImageLoadRequest req : list) {
				if (req != originReq) req.setFetchingProgressIfRequired(progress, total);
			}
		}
	}

	public void clear () {
		this.reqs.clear();
	}

	private void cleanUp () {
		final Iterator<List<ImageLoadRequest>> ittr = this.reqs.values().iterator();
		while (ittr.hasNext()) {
			final List<ImageLoadRequest> list = ittr.next();
			synchronized (list) {
				removeExpired(list.iterator());
				if (list.size() < 1) ittr.remove();
			}
		}
		if (this.reqs.size() > 100) LOG.e("Manager has %s active requests, is there a leak?", this.reqs.size());
	}

	private static void removeExpired (final Iterator<ImageLoadRequest> ittr) {
		while (ittr.hasNext()) {
			if (!ittr.next().isRequired()) ittr.remove();
		}
	}

}
