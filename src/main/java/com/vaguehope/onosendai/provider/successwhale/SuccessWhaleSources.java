package com.vaguehope.onosendai.provider.successwhale;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaguehope.onosendai.util.ArrayHelper;
import com.vaguehope.onosendai.util.StringHelper;

public class SuccessWhaleSources {

	private static final String SOURCE_SEP = ":";

	private final List<SuccessWhaleSource> sources;

	public SuccessWhaleSources (final List<SuccessWhaleSource> sources) {
		this.sources = Collections.unmodifiableList(sources);
	}

	public List<SuccessWhaleSource> getSources () {
		return this.sources;
	}

	public static String toResource (final Collection<SuccessWhaleSource> sources) {
		final List<String> fullurls = new ArrayList<String>();
		for (final SuccessWhaleSource source : sources) {
			fullurls.add(source.getFullurl());
		}
		return ArrayHelper.join(fullurls, SOURCE_SEP);
	}

	public static Set<SuccessWhaleSource> fromResource(final String resource) {
		if (StringHelper.isEmpty(resource)) return null;
		final Set<SuccessWhaleSource> ret = new HashSet<SuccessWhaleSource>();
		for (final String res : resource.split(SOURCE_SEP)) {
			ret.add(new SuccessWhaleSource(res, res));
		}
		return ret;
	}

}
