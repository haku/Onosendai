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

	public int size () {
		return this.predicates.size();
	}

	public boolean matches (final String body, final String username, final String ownerUsername) {
		if (StringHelper.isEmpty(body) && StringHelper.isEmpty(username)) return false;

		final String retweetByUsername;
		if (username == null || ownerUsername == null || ownerUsername.equalsIgnoreCase(username)) {
			retweetByUsername = null;
		}
		else {
			retweetByUsername = ownerUsername;
		}

		final String lowerCaseBody = body != null ? body.toLowerCase(Locale.ENGLISH) : null;
		final String lowerCaseUsername = username != null ? username.toLowerCase(Locale.ENGLISH) : null;
		final String lowerCaseRetweetByUsername = retweetByUsername != null ? retweetByUsername.toLowerCase(Locale.ENGLISH) : null;

		for (final Predicate predicate : this.predicates) {
			if (predicate.matches(lowerCaseBody, lowerCaseUsername, lowerCaseRetweetByUsername)) return true;
		}
		return false;
	}

	public List<Tweet> matchAndSet (final List<Tweet> input) {
		final List<Tweet> ret = new ArrayList<Tweet>(input.size());
		for (final Tweet tweet : input) {
			ret.add(tweet.withFiltered(matches(tweet.getBody(), tweet.getUsername(), tweet.getOwnerUsername())));
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
			try {
				ret.add(parseFilter(filter));
			}
			catch (final PatternSyntaxException e) {
				LOG.w("Invalid filter: %s", filter);
			}
		}
		return ret;
	}

	private static Predicate parseFilter (final String filter) {
		if (filter.startsWith("/")) return new Regex(filter);
		return new PlainString(filter);
	}

	public static void validateFilter (final String input) {
		parseFilter(input);
	}

	private interface Predicate {
		boolean matches (String lowerCaseBody, String lowerCaseUsername, String lowerCaseRetweetByUsername);
	}

	private static class PlainString implements Predicate {

		private final String lookFor;

		public PlainString (final String lookFor) {
			this.lookFor = lookFor.toLowerCase(Locale.ENGLISH);
		}

		@Override
		public boolean matches (final String lowerCaseBody, final String lowerCaseUsername, final String lowerCaseRetweetByUsername) {
			if (lowerCaseBody == null) return false;
			return lowerCaseBody.contains(this.lookFor);
		}

	}

	private static class Regex implements Predicate {

		private final Pattern bodyPattern;
		private final Pattern usernamePattern;
		private final Pattern retweetByPattern;

		public Regex (final String input) {
			if (!input.startsWith("/")) throw new IllegalArgumentException("Regex input does not start with '/': " + input);
			final int lastSlash = input.lastIndexOf('/');
			final String rawP = input.substring(1, lastSlash > 1 ? lastSlash : input.length());
			if (rawP.length() < 1) throw new PatternSyntaxException("Empty pattern.", rawP, 0);
			final Pattern p = Pattern.compile(rawP, Pattern.CASE_INSENSITIVE);
			final String flags = lastSlash > 1 ? input.substring(lastSlash + 1) : null;
			this.bodyPattern = flags == null || flags.length() < 1 || flags.contains("b") ? p : null;
			this.usernamePattern = flags != null && flags.length() > 0 && flags.contains("u") ? p : null;
			this.retweetByPattern = flags != null && flags.length() > 0 && flags.contains("r") ? p : null;
		}

		@Override
		public boolean matches (final String lowerCaseBody, final String lowerCaseUsername, final String lowerCaseRetweetByUsername) {
			return (this.bodyPattern != null && lowerCaseBody != null && this.bodyPattern.matcher(lowerCaseBody).find())
					|| (this.usernamePattern != null && lowerCaseUsername != null && this.usernamePattern.matcher(lowerCaseUsername).find())
					|| (this.retweetByPattern != null && lowerCaseRetweetByUsername != null && this.retweetByPattern.matcher(lowerCaseRetweetByUsername).find());
		}

	}

}
