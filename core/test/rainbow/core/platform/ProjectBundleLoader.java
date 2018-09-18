package rainbow.core.platform;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Map;

import javax.xml.bind.JAXBException;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

import rainbow.core.bundle.Bundle;
import rainbow.core.bundle.BundleClassLoader;
import rainbow.core.bundle.BundleData;

public class ProjectBundleLoader extends JarBundleLoader {

	@Override
	public Map<String, Bundle> loadBundle(Predicate<String> exist) {
		Map<String, Bundle> bundles = super.loadBundle(exist);
		File dir = new File("../");
		File[] files = dir.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory() && !file.getName().equals("core") && !file.getName().startsWith(".");
			}
		});
		if (bundles.isEmpty())
			bundles = Maps.newHashMapWithExpectedSize(files.length);
		for (File file : files)
			processProject(file, exist, bundles);
		return bundles;
	}

	private void processProject(File projectDir, Predicate<String> exist, Map<String, Bundle> map) {
		File root = new File(projectDir, "bin");
		BundleData data = null;
		try {
			File file = new File(root, "bundle.xml");
			if (!file.exists())
				return;
			data = binder.unmarshal(file);
		} catch (IOException e) {
			return;
		} catch (JAXBException e) {
			logger.error("parse xml[{}] failed", projectDir.getName(), e);
			return;
		}
		if (!projectDir.getName().equals(data.getId())) {
			logger.error("bundle id [{}] is not match the project name [{}]", data.getId(), projectDir.getName());
			return;
		}
		if (exist.apply(data.getId())) {
			return;
		}
		if (map.containsKey(data.getId()))
			logger.warn("duplicated bundle [{}] found, use project version [{}]", data.getId(), projectDir.getName());
		try {
			BundleClassLoader classLoader = new ProjectClassLoader(root);
			String[] libs = data.getLibs();
			if (libs != null && libs.length > 0) {
				File[] files = new File[libs.length];
				for (int i = 0; i < libs.length; i++) {
					files[i] = new File(projectDir, "lib/" + libs[i]);
				}
				classLoader.setPrivateLibs(files);
			}
			map.put(data.getId(), new Bundle(data, classLoader));
			logger.debug("find new bundle [{}]", data.getId());
		} catch (IOException e) {
			logger.error("make classLoader of [{}] failed", projectDir.getName(), e);
			return;
		}
	}

}
