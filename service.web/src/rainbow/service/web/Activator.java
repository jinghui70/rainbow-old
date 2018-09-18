package rainbow.service.web;

import java.util.List;

import com.google.common.collect.ImmutableList;

import rainbow.core.bundle.BundleActivator;

public class Activator extends BundleActivator {
	
	@Override
	public List<String> getParentContextId() {
		return ImmutableList.<String>of("service");
	}

}
