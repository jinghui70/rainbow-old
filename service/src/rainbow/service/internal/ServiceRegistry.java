package rainbow.service.internal;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import rainbow.core.bundle.Bean;
import rainbow.core.extension.ExtensionRegistry;
import rainbow.core.model.exception.AppException;
import rainbow.core.platform.Platform;
import rainbow.core.platform.Session;
import rainbow.core.util.Utils;
import rainbow.core.util.ioc.ActivatorAwareObject;
import rainbow.core.util.ioc.InitializingBean;
import rainbow.service.Internal;
import rainbow.service.ServiceInterceptor;
import rainbow.service.ServiceInvoker;
import rainbow.service.ServiceRequest;
import rainbow.service.ServiceResult;
import rainbow.service.SessionNotSetException;

/**
 * 服务注册表
 * 
 * @author lijinghui
 * 
 */
@Bean(name = "serviceInvoker")
public final class ServiceRegistry extends ActivatorAwareObject implements InitializingBean, ServiceInvoker {

	private Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

	/** 本地服务 */
	private ConcurrentMap<String, Service> services = new ConcurrentHashMap<String, Service>();

	private List<String> sessions;

	@Override
	public void afterPropertiesSet() {
		sessions = activator.getConfigAsList("session");
	}

	/**
	 * 返回指定名称的本地服务
	 * 
	 * @param serviceId
	 *            服务的ID
	 * @return 服务对象
	 */
	public Service getService(String serviceId) {
		Service service = services.get(serviceId);
		checkNotNull(service, "Service [%s] not found", serviceId);
		return service;
	}

	public List<Service> getServices(final String prefix) {
		Predicate<Service> predicate = Predicates.alwaysTrue();
		if (prefix != null && !prefix.isEmpty())
			predicate = new Predicate<Service>() {
				@Override
				public boolean apply(Service input) {
					return input.getServiceId().startsWith(prefix);
				}
			};
		Iterable<Service> result = Collections2.filter(services.values(), predicate);
		return Ordering.usingToString().sortedCopy(result);
	}

	/**
	 * 注册一个服务定义
	 * 
	 * @param serviceClass
	 *            服务定义接口类，必须是一个接口
	 * @return 服务的ID
	 */
	public String registerService(Class<?> serviceClass) {
		checkNotNull(serviceClass, "null service def class");
		checkArgument(serviceClass.isInterface(), "[%s] is not an interface", serviceClass.getName());
		String serviceId = findServiceId(serviceClass.getName(), true);
		checkArgument(!services.containsKey(serviceId), "duplicated service id of [%s]", serviceId);
		Service service = new Service(serviceId, serviceClass);
		services.put(serviceId, service);
		logger.info("service [{}] registered", serviceId);
		return serviceId;
	}

	/**
	 * 注销一个服务定义
	 * 
	 * @param serviceId
	 */
	public void unregisterService(String serviceId) {
		services.remove(serviceId);
		logger.info("service [{}] unregistered", serviceId);
	}

	/**
	 * 根据类名获取服务的Id号。
	 * 
	 * @param serviceClassName
	 *            服务接口或服务实现类名
	 * @param isDef
	 *            <code>true</code>为接口，<code>false</code>为实现
	 * @return
	 */
	public String findServiceId(String serviceClassName, boolean isDef) {
		String suffix = isDef ? "Service" : "ServiceImpl";
		String flag = isDef ? ".api." : ".impl.";
		serviceClassName = Platform.asProduct(serviceClassName);
		try {
			checkArgument(serviceClassName.endsWith(suffix));
			int index = serviceClassName.indexOf(flag);
			checkArgument(index > 0);
			String part1 = serviceClassName.substring(0, index + 1);
			String part2 = serviceClassName.substring(index + flag.length());
			checkArgument(!part2.isEmpty());
			LinkedList<String> idParts = Lists.newLinkedList();
			Iterator<String> i = Splitter.on('.').split(part1 + part2).iterator();
			while (i.hasNext()) {
				idParts.add(i.next());
			}
			String last = idParts.removeLast();
			last = last.substring(0, last.length() - suffix.length()).toLowerCase();
			checkArgument(idParts.getLast().equals(last));
			return Joiner.on('.').join(idParts);
		} catch (Throwable e) {
			throw new AppException("[%s] is not a valid %s name", serviceClassName, suffix);
		}
	}

	/**
	 * 处理对本地服务的一个请求
	 * 
	 * @param request
	 * @return 返回执行结果
	 */
	@Override
	public ServiceResult invoke(ServiceRequest request) {
		List<ServiceInterceptor> interceptors = ExtensionRegistry.getExtensionObjects(ServiceInterceptor.class);
		try {
			Service service = getService(request.getService());
			checkState(!service.isInternal(), "不能调用内部服务[%s]", request.getService());

			Method method = service.getMethod(request.getMethod());
			checkState(method.getAnnotation(Internal.class) == null, "不能调用服务[%s]的内部函数[%s]", request.getService(),
					request.getMethod());
			checkSession(service, method);
			Object target = service.getServiceImpl();

			for (ServiceInterceptor interceptor : interceptors) {
				try {
					interceptor.beforeService(service.getServiceClass(), method, request);
				} catch (Throwable e) {
					logger.error("service interceptor[{}] error before invoke", interceptor.getClass().getName(), e);
					if (!interceptor.ignoreBeforeException())
						throw e;
				}
			}
			Object result = method.invoke(target, request.getArgs());
			for (ServiceInterceptor interceptor : interceptors) {
				try {
					interceptor.afterService(service.getServiceClass(), method, request, result);
				} catch (Throwable e) {
					logger.error("service interceptor[{}] error after invoke", interceptor.getClass().getName(), e);
					if (!interceptor.ignoreAfterException())
						throw e;
				}
			}
			return ServiceResult.success(result);
		} catch (SessionNotSetException e) {
			return ServiceResult.noSession(e);
		} catch (InvocationTargetException e) {
			Throwable t = e.getTargetException();
			logger.error("invoking [{}:{}] failed: {}", request.getService(), request.getMethod(), t.getMessage(), t);
			return ServiceResult.exception(t);
		} catch (Throwable e) {
			logger.error("invoking [{}:{}] failed: {}", request.getService(), request.getMethod(), e.getMessage(), e);
			return ServiceResult.exception(e);
		}
	}

	private void checkSession(Service service, Method method) {
		List<String> checkSession = null;
		rainbow.service.Session s = method.getAnnotation(rainbow.service.Session.class);
		if (s != null) {
			checkSession = Lists.newArrayList(s.value()); // method 优先
		} else if (service.getSession() != null) {
			checkSession = service.getSession(); // service上的配置其次
		} else
			checkSession = sessions; // 最后看全局配置
		if (Utils.isNullOrEmpty(checkSession))
			return;
		for (String str : checkSession) {
			if (!Session.hasValue(str))
				throw new SessionNotSetException(str);
		}
	}

	@Override
	public Method getMethod(String serviceId, String methodName) {
		Service service = getService(serviceId);
		Method method = service.getMethod(methodName);
		checkNotNull(method, "Service[%s]-Method[%s] not found", serviceId, methodName);
		return method;
	}

	@Override
	public boolean hasService(String serviceId) {
		return services.get(serviceId) != null;
	}

	@Override
	public boolean hasMethod(String serviceId, String methodName) {
		Service service = services.get(serviceId);
		if (service == null) 
			return false;
		return service.getMethod(methodName) != null;
	}

}
