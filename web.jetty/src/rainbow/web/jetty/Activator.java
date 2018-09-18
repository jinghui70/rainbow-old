package rainbow.web.jetty;

import rainbow.core.bundle.BundleActivator;
import rainbow.core.bundle.BundleException;

public class Activator extends BundleActivator {

	private EmbedJetty embedJetty;

	@Override
	protected void doStart() throws BundleException {
		embedJetty = new EmbedJetty();
		try {
			embedJetty.start();
		} catch (Throwable e) {
			throw new BundleException("启动jetty server失败", e);
		}
	}

	@Override
	public void stop() {
		embedJetty.stop();
		super.stop();
	}

}
