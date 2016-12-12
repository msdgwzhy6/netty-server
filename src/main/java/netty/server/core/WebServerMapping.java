package netty.server.core;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;

import io.netty.handler.codec.http.*;
import javassist.*;
import javassist.Modifier;
import javassist.bytecode.*;

/**
 * URL映射实体
 */
class WebServerMapping {

	final Class<?> clazz;
	final Method method;
	final String[] names;

	WebServerMapping(final Class<?> clazz, final Method method) throws Exception {
		this.clazz = clazz;
		this.method = method;

		// 使用增强反射工具，还原出参数名，在服务器启动时预处理，可以提升运行时速度
		final ClassPool cp = ClassPool.getDefault();
		cp.insertClassPath(new ClassClassPath(clazz));
		final CtClass cc = cp.get(clazz.getName());
		final CtMethod cm = cc.getDeclaredMethod(method.getName());

		final MethodInfo methodInfo = cm.getMethodInfo();
		final CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
		final LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);

		names = new String[method.getParameterTypes().length];

		if (attr != null) {
			int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;
			for (int i = 0; i < names.length; i++)
				names[i] = attr.variableName(i + pos);
		}
	}

	static WebServerMapping get(final HttpRequest request, final String uri) {
		WebServerMapping mapping = null;

		if (request.method() == HttpMethod.GET) {
			mapping = WebServer.GET_MAPPING.get(uri); // 完全匹配
		} else if (request.method() == HttpMethod.POST) {
			mapping = WebServer.POST_MAPPING.get(uri); // 完全匹配
		}

		if (mapping != null)
			return mapping;

		if (request.method() == HttpMethod.GET) {
			mapping = get(uri, WebServer.GET_WILDCARDS); // 通配
		} else if (request.method() == HttpMethod.POST) {
			mapping = get(uri, WebServer.POST_WILDCARDS); // 通配
		}

		return mapping;
	}
	
	private static WebServerMapping get(String uri, Map<String, WebServerMapping> mapping) {
		for (Entry<String, WebServerMapping> item : mapping.entrySet())
			if (uri.matches(item.getKey()))
				return item.getValue();

		return null;
	}
}