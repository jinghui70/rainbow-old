package rainbow.core.platform;

import javax.management.ObjectName;

import com.google.common.base.Throwables;

public class PlatformManager implements PlatformManagerMBean {

	public static ObjectName getName() {
		try {
			return ObjectName.getInstance("rainbow:name=platform-manager");
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	@Override
	public void shutdown() {
		Platform.shutdown();
	}

}
