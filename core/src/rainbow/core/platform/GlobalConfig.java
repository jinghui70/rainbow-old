package rainbow.core.platform;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import rainbow.core.model.exception.AppException;

public class GlobalConfig {

	private JSONObject root;

	public GlobalConfig(File home) {
		File configFile = new File(home, "conf/core.json");
		try {
			root = JSON.parseObject(Files.toString(configFile, Charsets.UTF_8));
		} catch (FileNotFoundException e) {
			throw new AppException("core.json not exist, check rainbow home path");
		} catch (IOException e) {
			throw Throwables.propagate(e);
		}
	}

	public Optional<String> getConfig(String bundleId, String configItem) {
		JSONObject c = root.getJSONObject(bundleId);
		if (c == null)
			return Optional.absent();
		return Optional.fromNullable(c.getString(configItem));
	}

	public int getConfigAsInt(String bundleId, String configItem, int defValue) {
		JSONObject c = root.getJSONObject(bundleId);
		if (c == null)
			return defValue;
		return c.containsKey(configItem) ? c.getIntValue(configItem) : defValue;
	}

	public Optional<Integer> getConfigAsInt(String bundleId, String configItem) {
		JSONObject c = root.getJSONObject(bundleId);
		if (c == null)
			return Optional.absent();
		return Optional.fromNullable(c.getInteger(configItem));
	}

	public Map<String, String> getConfigAsMap(String bundleId, String configItem) {
		JSONObject c = root.getJSONObject(bundleId);
		if (c == null || c.isEmpty())
			return ImmutableMap.<String, String> of();
		c = c.getJSONObject(configItem);
		if (c == null || c.isEmpty())
			return ImmutableMap.<String, String> of();
		ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
		for (String key : c.keySet())
			builder.put(key, c.getString(key));
		return builder.build();
	}

	public <T> Optional<T> getConfigAsObject(String bundleId, String configItem, Class<T> clazz) {
		JSONObject c = root.getJSONObject(bundleId);
		if (c == null)
			return Optional.absent();
		return Optional.fromNullable(c.getObject(configItem, clazz));
	}
}
