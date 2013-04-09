package com.vaguehope.onosendai.provider.successwhale;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class EnabledServiceRefs {

	private final Set<ServiceRef> enabledRefs;
	private volatile boolean servicesPreSpecified;

	public EnabledServiceRefs () {
		this.enabledRefs = Collections.synchronizedSet(new HashSet<ServiceRef>());
	}

	public void enable (final ServiceRef ref) {
		this.enabledRefs.add(ref);
	}

	public void enableExclusiveAndSetPreSpecified (final ServiceRef ref) {
		synchronized (this.enabledRefs) {
			this.enabledRefs.clear();
			this.enabledRefs.add(ref);
			setServicesPreSpecified(true);
		}
	}

	public void disable (final ServiceRef ref) {
		this.enabledRefs.remove(ref);
	}

	public boolean isEnabled (final ServiceRef ref) {
		return this.enabledRefs.contains(ref);
	}

	public void setServicesPreSpecified (final boolean servicesPreSpecified) {
		this.servicesPreSpecified = servicesPreSpecified;
	}

	public boolean isServicesPreSpecified () {
		return this.servicesPreSpecified;
	}

	public Set<ServiceRef> copyOfServices () {
		synchronized (this.enabledRefs) {
			return Collections.unmodifiableSet(new HashSet<ServiceRef>(this.enabledRefs));
		}
	}

}
