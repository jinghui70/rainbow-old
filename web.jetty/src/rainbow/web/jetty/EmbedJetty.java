package rainbow.web.jetty;

import java.io.File;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rainbow.core.platform.Platform;

import com.google.common.base.Throwables;

public class EmbedJetty {

	private static final Logger logger = LoggerFactory.getLogger(EmbedJetty.class);

	private Server server;

	public void start() {
		String webRootDir = System.getProperty("RAINBOW_WEB_DIR", "web");
		int port = Integer.parseInt(System.getProperty("RAINBOW_WEB_PORT", "8080"));
		logger.info("web dir = [{}], port = {}", webRootDir, port);

		File webdir = new File(Platform.getHome(), webRootDir);
		if (!webdir.exists()) {
			logger.warn("webdir [{}] not exist, jetty not started", webdir.toString());
			return;
		}
		server = new Server(port);
		WebAppContext webapp = new WebAppContext();
		webapp.setContextPath("/");
		webapp.setResourceBase(webdir.getPath());
		server.setHandler(webapp);
		try {
			server.start();
		} catch (Exception e) {
			logger.error("start jetty server({}) at ({}) failed", new Object[] { webRootDir, port, e });
			Throwables.propagate(e);
		}
		logger.info("embedded jetty server started...");
	}

	public void stop() {
		if (server != null) {
			logger.info("stop embedded jetty server");
			try {
				server.stop();
			} catch (Throwable e) {
			}
			server = null;
		}
	}
}
