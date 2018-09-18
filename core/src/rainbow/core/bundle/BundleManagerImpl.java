package rainbow.core.bundle;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import javax.management.MBeanServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;

import rainbow.core.extension.Extension;
import rainbow.core.extension.ExtensionRegistry;
import rainbow.core.platform.BundleAncestor;
import rainbow.core.platform.BundleLoader;
import rainbow.core.util.Utils;
import rainbow.core.util.ioc.Context;
import rainbow.core.util.ioc.DisposableBean;
import rainbow.core.util.ioc.Inject;

public final class BundleManagerImpl implements BundleManager, DisposableBean {

	private static Logger logger = LoggerFactory.getLogger(BundleManagerImpl.class);

	private ConcurrentMap<String, Bundle> bundleMap = new MapMaker().concurrencyLevel(1).makeMap();

	private Multimap<Bundle, Bundle> bundleChildren = LinkedListMultimap.create();

	private BundleLoader bundleLoader;

	private MBeanServer mBeanServer;

	@Inject
	public void setBundleLoader(BundleLoader bundleLoader) {
		this.bundleLoader = bundleLoader;
	}

	@Inject
	public void setmBeanServer(MBeanServer mBeanServer) {
		this.mBeanServer = mBeanServer;
	}

	public synchronized void refresh() {
		Map<String, Bundle> newBundles = bundleLoader.loadBundle(new Predicate<String>() {
			@Override
			public boolean apply(String input) {
				if (bundleMap.containsKey(input))
					return true;
				return false;
			}
		});
		logger.info("found {} new bundles", newBundles.size());
		for (Entry<String, Bundle> entry : newBundles.entrySet()) {
			bundleMap.put(entry.getKey(), entry.getValue());
		}
		refreshUnactiveBundles();
	}

	/**
	 * 删除掉一个bundle
	 * 
	 * @param id
	 */
	@Override
	public synchronized void uninstallBundle(String id) {
		Bundle bundle = bundleMap.get(id);
		checkNotNull(bundle, "bundle [%s] not found", id);
		if (bundle.getState() == BundleState.FOUND || bundle.getState() == BundleState.READY) {
			bundleMap.remove(id);
			bundle.destroy();
		}
		refreshUnactiveBundles();
	}

	@Override
	public void destroy() throws Exception {
		stopAll();
		for (Bundle bundle : bundleMap.values())
			bundle.destroy();
		bundleMap.clear();
	}

	/**
	 * 返回指定的Bundle
	 * 
	 * @param id
	 * @return
	 */
	public Bundle get(String id) {
		return bundleMap.get(id);
	}

	/**
	 * 返回所有的bundle列表
	 * 
	 * @return
	 */
	public Collection<Bundle> getBundles() {
		return Ordering.usingToString().sortedCopy(bundleMap.values());
	}

	/**
	 * 返回满足条件的bundle列表
	 * 
	 * @return
	 */
	public Collection<Bundle> getBundles(Predicate<Bundle> predicate) {
		Collection<Bundle> bundles = getBundles();
		if (predicate == null)
			return bundles;
		return Collections2.filter(bundles, predicate);
	}

	/**
	 * 当发现了新bundle或者删掉了一个bundle，要重新计算bundle的解析状态
	 */
	private void refreshUnactiveBundles() {
		Collection<Bundle> bundles = bundleMap.values();
		for (Bundle bundle : bundles)
			if (bundle.getState() == BundleState.READY || bundle.getState() == BundleState.ERROR)
				bundle.setState(BundleState.FOUND);
		for (Bundle bundle : bundles)
			resolveBundle(bundle);
	}

	/**
	 * 解析一个包所依赖的所有父包
	 * 
	 * @param bundle
	 * @return
	 */
	private boolean resolveBundle(Bundle bundle) {
		if (bundle.getState() != BundleState.FOUND)
			return false;
		bundle.setState(BundleState.RESOLVING);
		try {
			bundle.setParents(null);
			if (bundle.getParentId() == null || bundle.getParentId().length == 0) {
				bundle.setState(BundleState.READY);
				return true;
			}
			BundleAncestor ancestor = new BundleAncestor();
			for (String id : bundle.getParentId()) {
				Bundle parent = get(id);
				if (parent == null)
					return false;
				if (ancestor.unaware(parent)) {
					if (parent.getState() == BundleState.FOUND) {
						if (!resolveBundle(parent))
							return false;
					}
					ancestor.addParent(parent);
				}
			}
			bundle.setState(BundleState.READY);
			bundle.setParents(ancestor.getParents());
			bundle.setAncestors(ancestor.getAncestors());
			return true;
		} finally {
			if (bundle.getState() != BundleState.READY) {
				bundle.setState(BundleState.FOUND);
				bundle.setParents(null);
			}
		}
	}

	public boolean startBundle(String id) throws BundleException {
		Bundle bundle = get(id);
		checkNotNull(bundle, "bundle [%s] not found", id);
		synchronized (this) {
			return startBundle(bundle);
		}
	}

	private boolean startBundle(Bundle bundle) {
		logger.info("starting bundle[{}]...", bundle.getId());
		if (bundle.getState() == BundleState.ACTIVE) {
			logger.info("bundle[{}] already started", bundle.getId());
			return true;
		}
		if (bundle.getState() != BundleState.READY) {
			logger.info("start bundle[{}] failed, bundle not ready", bundle.getId());
			return false;
		}
		for (Bundle parent : bundle.getParents()) {
			if (!startBundle(parent)) {
				logger.debug("start bundle[{}] failed, cannot start parent[{}]", bundle.getId(), parent.getId());
				return false;
			}
		}
		bundle.setState(BundleState.STARTING);
		try {
			doStartBundle(bundle);
			bundle.setState(BundleState.ACTIVE);
			logger.info("bundle[{}] started!", bundle.getId());
			fireBundleEvent(bundle, true);
			return true;
		} catch (Throwable e) {
			logger.error("start bundle[{}] failed", bundle.getId(), e);
			stopBundle(bundle);
			bundle.setState(BundleState.ERROR);
			return false;
		}
	}

	private void doStartBundle(Bundle bundle) throws BundleException {
		BundleActivator activator = bundle.createActivator();
		List<String> ids = activator.getParentContextId();
		Context[] parentContexts = new Context[ids.size()];
		int i = 0;
		for (String id : ids) {
			Bundle contextBundle = get(id);
			checkNotNull(contextBundle, "can not find parent context bundle [%s]", id);
			checkState(bundle.getAncestors().contains(contextBundle), "can not make context [%s] as parent context",
					id);
			Context parentContext = contextBundle.activator.getContext();
			checkNotNull(parentContext, "can not find parent context [%s]", id);
			parentContexts[i++] = parentContext;
		}
		bundle.setActivator(activator);
		activator.start(mBeanServer, parentContexts);
		for (Bundle parent : bundle.getParents()) {
			bundleChildren.put(parent, bundle);
		}
	}

	public void stopBundle(String id) throws BundleException {
		Bundle bundle = get(id);
		checkNotNull(bundle, "bundle [%s] not found", id);
		synchronized (this) {
			stopBundle(bundle);
		}
	}

	private void stopBundle(Bundle bundle) {
		if (bundle.getState() != BundleState.ACTIVE)
			return;
		logger.debug("stopping bundle [{}]...", bundle.getId());
		bundle.setState(BundleState.STOPPING);
		// stopping children first
		for (Bundle child : ImmutableList.copyOf(bundleChildren.get(bundle))) {
			stopBundle(child);
		}
		bundleChildren.removeAll(bundle);

		// stop self
		if (bundle.activator != null) {
			bundle.activator.stop();
			bundle.setActivator(null);
		}
		for (Bundle parent : bundle.getParents())
			bundleChildren.remove(parent, bundle);
		bundle.setState(BundleState.READY);

		logger.info("bundle [{}] stopped!", bundle.getId());
		fireBundleEvent(bundle, false);
	}

	/**
	 * 把字符串列表转换为一个Predicate
	 * 
	 * @param strings
	 * @return
	 */
	private Predicate<String> toPredicate(List<String> strings) {
		if (strings.isEmpty())
			return null;
		Predicate<String> result = Predicates.alwaysFalse();
		for (String str : strings) {
			int len = str.length();
			if (str.charAt(len - 1) == '*') {
				result = Predicates.or(result, Utils.startWith(str.substring(0, len - 1)));
			} else
				result = Predicates.or(result, Predicates.equalTo(str));
		}
		return result;
	}

	/**
	 * 初始化启动的bundle
	 * 
	 * @param onList
	 *            初始化要启动的bundle列表
	 * @param offList
	 *            初始化不要启动的bundle列表
	 */
	public void initStart(List<String> onList, List<String> offList) {
		Collection<Bundle> bundles = getBundles();
		Predicate<String> predicateOn = toPredicate(onList);
		Predicate<String> predicateOff = toPredicate(offList);

		Collection<Bundle> result = new ArrayList<Bundle>(bundles.size());

		for (Bundle bundle : bundles) {
			if (result.contains(bundle))
				continue;
			if (predicateOn != null && !predicateOn.apply(bundle.getId()))
				continue;
			if (predicateOff != null && predicateOff.apply(bundle.getId()))
				continue;
			for (Bundle parent : bundle.getAncestors()) {
				if (!result.contains(parent))
					result.add(parent);
			}
			result.add(bundle);
		}
		logger.info("starting bundles: {}",
				Joiner.on(',').join(Collections2.transform(result, Functions.toStringFunction())));
		for (Bundle bundle : result)
			startBundle(bundle);
	}

	/**
	 * 停止所有的bundle
	 */
	public synchronized void stopAll() {
		for (Bundle bundle : getBundles())
			stopBundle(bundle);
	}

	/**
	 * 发送bundle变化的消息
	 * 
	 * @param bundle
	 * @param active
	 */
	private void fireBundleEvent(Bundle bundle, boolean active) {
		for (Extension extension : ExtensionRegistry.getExtensions(BundleListener.class)) {
			Bundle extensionBundle = null;
			if (extension.getBundle() != null)
				extensionBundle = get(extension.getBundle());
			if (extensionBundle == null
					|| extensionBundle.getState() == BundleState.ACTIVE && extensionBundle != bundle) {
				BundleListener listener = (BundleListener) extension.getObject();
				try {
					if (active)
						listener.bundleStarted(bundle.getId());
					else
						listener.bundleStop(bundle.getId());
				} catch (Throwable e) {
					String msg = String.format("when bundle[%s] %s, listener [%s:%s] encounter an error",
							bundle.getId(), active ? "start" : "stop", extensionBundle.getId(), listener.getClass());
					logger.error(msg, e);
				}
			}
		}
	}

}
