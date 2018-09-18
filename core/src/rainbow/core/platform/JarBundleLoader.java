package rainbow.core.platform;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;

import rainbow.core.bundle.Bundle;
import rainbow.core.bundle.BundleData;
import rainbow.core.bundle.BundleException;
import rainbow.core.bundle.Resource;

public class JarBundleLoader implements BundleLoader {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public Map<String, Bundle> loadBundle(Predicate<String> exist) {
		File bundleDir = getBundleDir();
		if (!bundleDir.exists() || !bundleDir.isDirectory()) {
			logger.error("bundle directory [{}] not exists", bundleDir.getAbsolutePath());
			return Collections.emptyMap();
		}
		File[] files = bundleDir.listFiles(new FileFilter() {
			public boolean accept(File file) {
				String fileName = file.getName();
				return file.isFile() && fileName.endsWith(".jar");
			}
		});
		Map<String, Bundle> result = Maps.newHashMapWithExpectedSize(files.length);
		for (File file : files)
			processFile(file, exist, result, bundleDir);
		return result;
	}

	protected File getBundleDir() {
		return new File(Platform.getHome(), "bundle");
	}

	/**
	 * 从文件读取bundle
	 * 
	 * @param file
	 * @return
	 */
	private void processFile(File file, Predicate<String> exist, Map<String, Bundle> map, File bundleDir) {
		JarClassLoader classLoader = null;
		try {
			classLoader = new JarClassLoader(file);
			Resource r = classLoader.getLocalResource("bundle.xml");
			checkNotNull(r, "bundle.xml not found");

			BundleData data = null;
			InputStream is = null;
			try {
				is = r.getInputStream();
				data = binder.unmarshal(is);
			} finally {
				Closeables.closeQuietly(is);
			}
			if (file.getName().startsWith(data.getId())) {
				if (exist.apply(data.getId()))
					return;
				if (map.containsKey(data.getId()))
					logger.warn("duplicated bundle [{}] found, use version [{}]", data.getId(), file.getName());
				map.put(data.getId(), new Bundle(data, classLoader));
				String[] libs = data.getLibs();
				if (libs != null && libs.length > 0) {
					File[] files = new File[libs.length];
					for (int i = 0; i < libs.length; i++) {
						files[i] = new File(bundleDir, data.getId() + "/" + libs[i]);
					}
					classLoader.setPrivateLibs(files);
				}
				logger.debug("find new bundle[{}] at [{}]", data.getId(), file.getName());
			} else
				throw new BundleException("bundle id [%s] not match the jar file name [%s]", data.getId(),
						file.getName());
		} catch (Throwable e) {
			logger.error("load bundle file [{}] failed", file.getName(), e);
			classLoader.destroy();
		}
	}

}