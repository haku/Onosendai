package com.vaguehope.onosendai.provider.successwhale;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaguehope.onosendai.provider.ServiceRef;

import android.os.Bundle;

public class EnabledServiceRefs {

	private static final String KEY_SREFS = "enabled_service_refs";

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

	public void addToBundle (final Bundle bundle) {
		if (bundle == null) return;
		synchronized (this.enabledRefs) {
			final ArrayList<String> arr = new ArrayList<String>();
			for (ServiceRef sr : this.enabledRefs) {
				arr.add(sr.toServiceMeta());
			}
			bundle.putStringArrayList(KEY_SREFS, arr);
		}
	}

	public void fromBundle (final Bundle bundle) {
		if (bundle == null) return;
		synchronized (this.enabledRefs) {
			final List<String> arr = bundle.getStringArrayList(KEY_SREFS);
			if (arr == null) return;
			this.enabledRefs.clear();
			for (String svcMeta : arr) {
				this.enabledRefs.add(SuccessWhaleProvider.parseServiceMeta(svcMeta));
			}
			setServicesPreSpecified(true);
		}
	}

}
