package rainbow.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * 输入服务Id，求服务类名的Task
 * 
 * @author lijinghui
 * 
 */
public class Service extends Task {

	private String serviceId;

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	@Override
	public void execute() throws BuildException {
		if (serviceId == null) {
			throw new BuildException("No serviceId");
		}
		int index = serviceId.lastIndexOf('.');
		String serviceName = new StringBuilder().append(Character.toUpperCase(serviceId.charAt(index + 1)))
				.append(serviceId.substring(index + 2)).toString();
		String packagePath = serviceId.replace('.', '/').toLowerCase();
		getProject().setNewProperty("serviceName", serviceName);
		getProject().setNewProperty("serviceId", serviceId.toLowerCase());
		getProject().setNewProperty("packagePath", packagePath);
	}

}
