package netty.server.core;

import java.lang.reflect.*;

public class WebServerMapping {

	public Class<?> clazz;
	public Method method;

	public WebServerMapping() {
		super();
	}

	public WebServerMapping(Class<?> clazz, Method method) {
		this.clazz = clazz;
		this.method = method;
	}
}