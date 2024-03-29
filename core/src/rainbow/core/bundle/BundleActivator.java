package rainbow.core.bundle;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import rainbow.core.extension.Extension;
import rainbow.core.extension.ExtensionRegistry;
import rainbow.core.platform.Platform;
import rainbow.core.util.Utils;
import rainbow.core.util.ioc.Bean;
import rainbow.core.util.ioc.BundleContext;
import rainbow.core.util.ioc.Context;

/**
 * Bundle启动器
 * 
 * @author lijinghui
 * 
 */
public abstract class BundleActivator {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected MBeanServer mBeanServer;

	private String bundleId;

	protected Context context;

	private List<Class<?>> points;
	private List<Extension> extensions;
	private List<ObjectName> mBeanNames;

	public BundleClassLoader getClassLoader() {
		return (BundleClassLoader) getClass().getClassLoader();
	}

	public String getBundleId() {
		return bundleId;
	}

	public void setBundleId(String bundleId) {
		this.bundleId = bundleId;
	}

	public Context getContext() {
		return context;
	}

	/**
	 * Bundle启动
	 * 
	 * @param mBeanServer
	 * @param bundleId
	 * @throws BundleException
	 */
	public final void start(MBeanServer mBeanServer, Context[] parentContexts) throws BundleException {
		this.mBeanServer = mBeanServer;
		registerExtensionPoint();

		Map<String, Bean> contextConfig = new HashMap<String, Bean>();
		Map<String, Class<?>> extensionConfig = new HashMap<String, Class<?>>();
		// 读取@Bean配置
		initContextConfig(contextConfig, extensionConfig);
		// 加载Context
		if (!contextConfig.isEmpty()) {
			context = createContext(contextConfig, parentContexts);
			context.loadAll();
		}
		// 注册配置在@Bean中的扩展
		if (!extensionConfig.isEmpty()) {
			for (java.util.Map.Entry<String, Class<?>> entry : extensionConfig.entrySet()) {
				registerExtension(entry.getValue(), entry.getKey());
			}
		}
		registerExtension();
		doStart();
	}

	/**
	 * 注册所有的扩展点
	 */
	protected void registerExtensionPoint() throws BundleException {
	}

	/**
	 * 注册未配置的扩展
	 */
	protected void registerExtension() throws BundleException {
	}

	/**
	 * 其它初始化代码写在这里
	 * 
	 * @throws BundleException
	 */
	protected void doStart() throws BundleException {
	}

	/**
	 * 创建context
	 * 
	 * @param contextConfig
	 * @param parent
	 */
	protected Context createContext(Map<String, Bean> contextConfig, Context[] parent) {
		return new BundleContext(this, contextConfig, parent);
	}

	/**
	 * 对于自己的context，返回需要的上级Context对应的bundleId
	 * 
	 * @return
	 */
	public List<String> getParentContextId() {
		return Collections.emptyList();
	}

	/**
	 * 自动读取@Bean配置
	 * 
	 * @param contextConfig
	 * @param extensionConfig
	 * @throws BundleException
	 */
	protected void initContextConfig(final Map<String, Bean> contextConfig, final Map<String, Class<?>> extensionConfig)
			throws BundleException {
		getClassLoader().procResource(new ResourceProcessor() {
			@Override
			public void processResource(BundleClassLoader classLoader, Resource resource) throws BundleException {
				if (!resource.getName().endsWith(".class"))
					return;
				String className = Utils.substringBefore(resource.getName(), ".class").replace('/', '.');
				Class<?> clazz = null;
				try {
					clazz = classLoader.loadClass(className);
				} catch (ClassNotFoundException e) {
					throw new BundleException(e, e.getMessage());
				}
				rainbow.core.bundle.Bean beandef = clazz.getAnnotation(rainbow.core.bundle.Bean.class);
				if (beandef == null)
					return;
				String beanName = beandef.name();
				if (beanName.isEmpty()) {
					beanName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, clazz.getSimpleName());
					if (beanName.endsWith("Impl"))
						beanName = Utils.substringBefore(beanName, "Impl");
				}
				if (beandef.singleton()) {
					contextConfig.put(beanName, Bean.singleton(clazz));
					if (beandef.extension() != String.class) {
						extensionConfig.put(beanName, beandef.extension());
					}
				} else
					contextConfig.put(beanName, Bean.prototype(clazz));

			}
		});
	}

	/**
	 * Bundle停止
	 */
	public void stop() {
		if (points != null)
			for (Class<?> point : points)
				ExtensionRegistry.unregisterExtensionPoint(point);
		if (extensions != null)
			for (Extension extension : extensions)
				ExtensionRegistry.unregisterExtension(extension);
		if (mBeanNames != null)
			unregisterMBean();
		if (context != null)
			context.close();
	}

	/**
	 * 打开bundle中的一个资源
	 * 
	 * @param resource
	 * @return
	 * @throws IOException
	 */
	protected InputStream getResource(String resource) throws IOException {
		Resource r = getClassLoader().getLocalResource(resource);
		return r.getInputStream();
	}

	/**
	 * 返回Bundle的配置目录
	 * 
	 * @return
	 */
	private File getConfigurePath() {
		return new File(Platform.getHome(), "conf/" + bundleId);
	}

	/**
	 * 返回Bundle的配置目录下指定名字的配置文件对象
	 * 
	 * @param fileName
	 * @return
	 */
	public final File getConfigureFile(String fileName) {
		return new File(getConfigurePath(), fileName);
	}

	/**
	 * 返回Bundle的配置目录下指定后缀的所有文件
	 * 
	 * @param suffix
	 * @return
	 */
	public final File[] getConfigureFiles(final String suffix) {
		return getConfigurePath().listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(suffix);
			}
		});
	}

	/**
	 * 返回配置在统一配置文件里的配置
	 * 
	 * @param key
	 * @return
	 */
	public final Optional<String> getConfig(String key) {
		return Platform.getConfig().getConfig(bundleId, key);
	}

	/**
	 * 返回配置在统一配置文件里的配置
	 * 
	 * @param key
	 * @return
	 */
	public final Optional<Integer> getConfigAsInt(String key) {
		return Platform.getConfig().getConfigAsInt(bundleId, key);
	}

	/**
	 * 返回配置在统一配置文件里的配置
	 * 
	 * @param key
	 * @param defValue
	 *            不存在的时候返回的默认值
	 * @return
	 */
	public final int getConfigAsInt(String key, int defValue) {
		return Platform.getConfig().getConfigAsInt(bundleId, key, defValue);
	}

	/**
	 * 返回配置在统一配置文件里的配置的列表
	 * 
	 * @param key
	 * @return
	 */
	public final List<String> getConfigAsList(String key) {
		Optional<String> str = getConfig(key);
		if (str.isPresent())
			return Splitter.on(',').splitToList(str.get());
		else
			return ImmutableList.<String> of();
	}
	
	public final Map<String, String> getConfigAsMap(String key) {
		return Platform.getConfig().getConfigAsMap(bundleId, key);
	}

	public <T> Optional<T> getConfigAsObject(String key, Class<T> clazz) {
		return Platform.getConfig().getConfigAsObject(bundleId, key, clazz);
	}

	/**
	 * 注册扩展点
	 * 
	 * @param clazz
	 * @throws BundleException
	 */
	protected final void registerExtensionPoint(Class<?> clazz) throws BundleException {
		ExtensionRegistry.registerExtensionPoint(bundleId, clazz);
		if (points == null)
			points = new LinkedList<Class<?>>();
		points.add(clazz);
	}

	/**
	 * 注册一个扩展
	 * 
	 * @param clazz
	 *            扩展点
	 * @param object
	 *            扩展对象
	 * @throws BundleException
	 */
	protected final void registerExtension(Class<?> clazz, String name, Object object) throws BundleException {
		Extension extension = ExtensionRegistry.registerExtension(bundleId, clazz, name, object);
		if (extensions == null)
			extensions = new LinkedList<Extension>();
		extensions.add(extension);
	}

	/**
	 * 注册一个扩展
	 * 
	 * @param clazz
	 *            扩展点
	 * @param beanName
	 *            在context中的扩展对象名
	 * @throws BundleException
	 */
	protected final <T> void registerExtension(Class<T> clazz, String beanName) throws BundleException {
		registerExtension(clazz, null, getBean(beanName));
	}

	protected final void registerMBean(Object mbean, String name) {
		try {
			ObjectName objName = new ObjectName("rainbow:name=" + name);
			mBeanServer.registerMBean(mbean, objName);
			if (mBeanNames == null)
				mBeanNames = new LinkedList<ObjectName>();
			mBeanNames.add(objName);
		} catch (Exception e) {
			logger.error("registerMBean {} failed", name, e);
		}
	}

	protected final void unregisterMBean() {
		if (mBeanNames == null)
			return;
		for (ObjectName objName : mBeanNames)
			try {
				mBeanServer.unregisterMBean(objName);
			} catch (Exception e) {
				logger.error("unregisterMBean {} failed", objName.getCanonicalName(), e);
			}
	}

	public final <T> T getBean(String name, Class<T> clazz) {
		if (context == null)
			return null;
		return context.getBean(name, clazz);
	}

	public final Object getBean(String name) {
		if (context == null)
			return null;
		return context.getBean(name);
	}

}
