package rainbow.service.web;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.ParserConfig;
import com.google.common.base.Splitter;
import com.google.common.io.CharStreams;

import rainbow.core.bundle.Bean;
import rainbow.core.util.Utils;
import rainbow.core.util.ioc.Inject;
import rainbow.core.web.Mapping;
import rainbow.core.web.UrlHandler;
import rainbow.service.ServiceInvoker;
import rainbow.service.ServiceRequest;
import rainbow.service.ServiceResult;

@Bean(extension = UrlHandler.class)
@Mapping("*.service")
public class ServiceHandler extends UrlHandler {

	private static final Logger logger = LoggerFactory.getLogger(ServiceHandler.class);

	private ServiceInvoker serviceInvoker;

	@Inject
	public void setServiceInvoker(ServiceInvoker serviceInvoker) {
		this.serviceInvoker = serviceInvoker;
	}

	@Override
	public boolean handle(ServletContext sc, HttpServletRequest servletRequest, HttpServletResponse servletResponse,
			String Path) throws IOException {
		ServiceResult result = null;
		ServiceRequest request = null;
		String requestStr = CharStreams.toString(servletRequest.getReader());
		HttpSession session = servletRequest.getSession();
		try {
			request = buildRequest(requestStr);
		} catch (Throwable e) {
			logger.error("parsing request [{}] failed.", requestStr, e);
			writeJsonBack(servletResponse, ServiceResult.exception(e));
		}
		prepareSession(session);
		result = serviceInvoker.invoke(request);
		writeJsonBack(servletResponse, result);
		return true;
	}

	private ServiceRequest buildRequest(String requestStr) {
		ServiceRequest request = new ServiceRequest();
		Iterator<String> i = Splitter.on(',').limit(3).split(requestStr).iterator();
		request.setService(i.next());
		request.setMethod(i.next());

		Method method = serviceInvoker.getMethod(request.getService(), request.getMethod());
		Type[] types = method.getGenericParameterTypes();
		if (types.length > 0) {
			DefaultJSONParser parser = new DefaultJSONParser(i.next(), ParserConfig.getGlobalInstance());
			request.setArgs(parser.parseArray(types));
			parser.close();

		} else {
			request.setArgs(Utils.NULL_ARRAY);
		}
		return request;
	}

}