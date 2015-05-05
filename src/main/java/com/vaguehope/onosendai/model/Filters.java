package com.vaguehope.onosendai.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.StringHelper;

public class Filters {

	private static final LogWrapper LOG = new LogWrapper("FL");

	private final Collection<Predicate> predicates;

	public Filters (final String... filters) {
		this(Arrays.asList(filters));
	}

	public Filters (final Collection<String> filters) {
		this.predicates = parseFilters(filters);
	}

	public int size() {
		return this.predicates.size();
	}

	public boolean matches (final String s) {
		if (StringHelper.isEmpty(s)) return false;
		final String lowerCase = s.toLowerCase(Locale.ENGLISH);
		for (final Predicate predicate : this.predicates) {
			if (predicate.matches(lowerCase)) return true;
		}
		return false;
	}

	public List<Tweet> matchAndSet (final List<Tweet> input) {
		final List<Tweet> ret = new ArrayList<Tweet>(input.size());
		for (final Tweet tweet : input) {
			ret.add(tweet.withFiltered(matches(tweet.getBody())));
		}
		return ret;
	}

	public static int countFiltered (final List<Tweet> input) {
		int ret = 0;
		for (final Tweet tweet : input) {
			if (tweet.isFiltered()) ret += 1;
		}
		return ret;
	}

	private static Collection<Predicate> parseFilters (final Collection<String> filters) {
		final List<Predicate> ret = new ArrayList<Filters.Predicate>();
		for (final String filter : filters) {
			if (StringHelper.isEmpty(filter)) continue;
			if (filter.startsWith("/")) {
				try {
					ret.add(new Regex(filter));
				}
				catch (final PatternSyntaxException e) {
					LOG.w("Invalid filter: %s", filter);
				}
			}
			else {
				ret.add(new PlainString(filter));
			}
		}
		return ret;
	}

	public static Pattern compileRegexFilter (final String input) {
		String raw = input;
		raw = raw.startsWith("/") ? raw.substring(1) : raw;
		raw = raw.endsWith("/") ? raw.substring(0, raw.length() - 1) : raw;
		return Pattern.compile(raw, Pattern.CASE_INSENSITIVE);
	}

	private interface Predicate {
		boolean matches (String lowerCase);
	}

	private static class PlainString implements Predicate {

		private final String lookFor;

		public PlainString (final String lookFor) {
			this.lookFor = lookFor.toLowerCase(Locale.ENGLISH);
		}

		@Override
		public boolean matches (final String lowerCase) {
			if (lowerCase == null) return false;
			return lowerCase.contains(this.lookFor);
		}

	}

	private static class Regex implements Predicate {

		private final Pattern pattern;

		public Regex (final String input) {
			this.pattern = compileRegexFilter(input);
		}

		@Override
		public boolean matches (final String lowerCase) {
			return this.pattern.matcher(lowerCase).find();
		}

	}

}
