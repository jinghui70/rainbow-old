package rainbow.core.platform;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import rainbow.core.bundle.BundleListener;
import rainbow.core.bundle.BundleManager;
import rainbow.core.bundle.BundleManagerImpl;
import rainbow.core.console.CommandProvider;
import rainbow.core.console.Console;
import rainbow.core.extension.ExtensionRegistry;
import rainbow.core.model.exception.AppException;
import rainbow.core.util.JMXServiceURLBuilder;
import rainbow.core.util.Utils;
import rainbow.core.util.encrypt.Encryption;
import rainbow.core.util.ioc.Bean;
import rainbow.core.util.ioc.Context;
import rainbow.core.web.UploadHandler;
import rainbow.core.web.UrlHandler;

/**
 * Rainbow 系统平台
 * 
 * @author lijinghui
 * 
 */
public final class Platform {

	private final static Logger logger = LoggerFactory.getLogger(Platform.class);

	private Platform() {
	}

	private static Platform platform;
	public static PlatformState state = PlatformState.READY;

	/**
	 * 根目录
	 */
	private File home;

	/**
	 * 平台id
	 */
	private int id;

	/**
	 * 全局配置信息
	 */
	private GlobalConfig config;

	/**
	 * 产品定义
	 */
	private Map<String, String> products;

	/**
	 * Rainbow 平台的启动入口
	 */
	public static void startup() {
		startup(true);
	}

	/**
	 * 启动Rainbow平台
	 */
	public static void startup(boolean startLocalJmxServer) {
		if (state != PlatformState.READY)
			return;
		state = PlatformState.STARTING;
		platform = new Platform();
		try {
			platform.doStart(startLocalJmxServer);
			state = PlatformState.STARTED;
		} catch (Throwable e) {
			platform = null;
			state = PlatformState.READY;
			logger.error("start rainbow failed", e);
			Throwables.propagate(e);
		}
	}

	/**
	 * 关闭Rainbow平台
	 */
	public static void shutdown() {
		if (state != PlatformState.STARTED)
			return;
		state = PlatformState.STOPPING;
		platform.doShutdown();
		state = PlatformState.READY;
		logger.info("Rainbow platform shutted down!");
	}

	private JMXConnectorServer cs;

	private Context context = new Context(ImmutableMap.of( //
			"mBeanServer", Bean.singleton(MBeanServerFactory.createMBeanServer(), MBeanServer.class), //
			"bundleLoader", Bean.singleton(BundleLoader.class), //
			"bundleManager", Bean.singleton(BundleManagerImpl.class), //
			"bundleCommandProvider", Bean.singleton(BundleCommandProvider.class) //
	));

	/**
	 * 启动平台
	 * 
	 * @param startLocalJmxServer
	 *            是否启动本地的JMX Server
	 */
	private void doStart(boolean startLocalJmxServer) throws Throwable {
		String homeStr = checkNotNull(System.getProperty("RAINBOW_HOME"), "RAINBOW_HOME must be set");
		home = new File(homeStr);
		logger.info("RAINBOW_HOME = {}", home.toString());
		logger.info("loading config param from core.json...");
		config = new GlobalConfig(home);
		Optional<Integer> idcfg = config.getConfigAsInt("core", "id");
		if (idcfg.isPresent())
			id = idcfg.get();
		else {
			throw new AppException("platform id not defined!");
		}
		logger.info("Rainbow ID = {}", getId());
		products = config.getConfigAsMap("core", "products");
		logger.info("products: {}", products.keySet().toString());
		setBundleLoader();
		if (startLocalJmxServer)
			startLocalJmxServer();

		// 注册扩展点
		ExtensionRegistry.registerExtensionPoint(null, BundleListener.class);
		ExtensionRegistry.registerExtensionPoint(null, Encryption.class);
		ExtensionRegistry.registerExtensionPoint(null, UrlHandler.class);
		ExtensionRegistry.registerExtensionPoint(null, UploadHandler.class);

		// 加密
		Optional<String> encryptionClass = config.getConfig("core", "encryption");
		if (encryptionClass.isPresent()) {
			try {
				Class.forName(encryptionClass.get());
			} catch (ClassNotFoundException e) {
				logger.warn("Encryption class [{}] not found!",encryptionClass);
			}
		}
		// 控制台
		Optional<String> _console = config.getConfig("core", "console");
		if (_console.isPresent() && "enabled".equals(_console.get())) {
			ExtensionRegistry.registerExtensionPoint(null, CommandProvider.class);
			ExtensionRegistry.registerExtension(null, CommandProvider.class,
					context.getBean(BundleCommandProvider.class));
			Console console = new Console();
			Thread t = new Thread(console, "Rainbow Console");
			t.setDaemon(true);
			t.start();
		}

		BundleManager bundleManager = context.getBean(BundleManager.class);
		bundleManager.refresh();
		bundleManager.initStart(getBundles("on"), getBundles("off"));
	}

	private List<String> getBundles(String onoff) {
		Optional<String> str = config.getConfig("core", onoff);
		if (str.isPresent()) {
			return Splitter.on(',').splitToList(str.get());
		} else
			return ImmutableList.<String> of();
	}

	/**
	 * 设定BundleLoader
	 * 
	 * @throws Exception
	 */
	private void setBundleLoader() throws Exception {
		BundleLoader bundleLoader = null;
		try {
			// 开发环境
			Class<?> clazz = Class.forName("rainbow.core.platform.ProjectBundleLoader");
			bundleLoader = (BundleLoader) clazz.newInstance();
		} catch (ClassNotFoundException e) {
			bundleLoader = new JarBundleLoader(); // 生产环境
		}
		context.setBean("bundleLoader", bundleLoader);
	}

	private void startLocalJmxServer() {
		int jmxPort = config.getConfigAsInt("core", "jmxPort", 1109);
		logger.error("start jmx server on port {}", jmxPort);
		// MBeanServer
		MBeanServer mBeanServer = context.getBean("mBeanServer", MBeanServer.class);
		try {
			JMXServiceURL url = new JMXServiceURLBuilder(jmxPort, "rainbow").getJMXServiceURL();
			cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mBeanServer);
			cs.start();
		} catch (IOException e) {
			logger.error("start jmx server failed", e);
			Throwables.propagate(e);
		}
		try {
			mBeanServer.registerMBean(new PlatformManager(), PlatformManager.getName());
		} catch (Exception e) {
			logger.error("register PlatformManager failed", e);
			Throwables.propagate(e);
		}
	}

	/**
	 * 关闭Rainbow平台
	 */
	public void doShutdown() {
		context.close();
		if (cs != null)
			try {
				cs.stop();
			} catch (IOException e) {
				logger.error("Stop JMX connect server failed", e);
			}
	}

	public static File getHome() {
		return platform.home;
	}

	public static int getId() {
		return platform.id;
	}

	public static String getProduct(String key) {
		return platform.products.get(key);
	}

	/**
	 * 根据字符串，找出其对应product的简称。
	 * 
	 * @param str
	 *            查询字符串，一般应是一个类名
	 * @return
	 */
	public static String getProductKey(String str) {
		for (Entry<String, String> entry : platform.products.entrySet()) {
			if (str.startsWith(entry.getValue()))
				return entry.getKey();
		}
		return null;
	}

	/**
	 * 把一个代表类名的字符串简化为product名开头的字符串
	 * 
	 * @param str
	 * @return
	 */
	public static String asProduct(String str) {
		for (Entry<String, String> entry : platform.products.entrySet()) {
			String value = entry.getValue();
			if (str.startsWith(value)) {
				return entry.getKey() + Utils.substringAfter(str, value);
			}
		}
		return str;
	}

	public static GlobalConfig getConfig() {
		return platform.config;
	}
}
