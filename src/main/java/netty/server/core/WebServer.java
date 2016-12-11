package netty.server.core;

import java.lang.reflect.*;
import java.util.*;

import io.netty.bootstrap.*;
import io.netty.channel.*;
import io.netty.channel.nio.*;
import io.netty.channel.socket.nio.*;
import io.netty.handler.logging.*;

import netty.server.annotation.*;
import netty.server.annotation.type.*;

/**
 * Web服务启动类
 */
public final class WebServer {

	private static final String BASE_PACKAGE = WebServerUtil.getProperties("server.properties", "basePackage");

	/**
	 * URL匹配映射(GET请求)
	 */
	static final Map<String, WebServerMapping> GET_MAPPING = new HashMap<String, WebServerMapping>();
	
	/**
	 * URL通配映射(GET请求)
	 */
	static final Map<String, WebServerMapping> GET_WILDCARDS = new HashMap<String, WebServerMapping>();

	/**
	 * URL匹配映射(POST请求)
	 */
	static final Map<String, WebServerMapping> POST_MAPPING = new HashMap<String, WebServerMapping>();
	
	/**
	 * URL通配映射(POST请求)
	 */
	static final Map<String, WebServerMapping> POST_WILDCARDS = new HashMap<String, WebServerMapping>();

	public static void run() throws Exception {
		String port = WebServerUtil.getProperties("server.properties", "port");
		run(port == null ? 80 : Integer.valueOf(port));
	}

	public static void run(int port) throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			init();

			ServerBootstrap b = new ServerBootstrap().group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO))
					.childHandler(new WebServerInitializer());

			Channel ch = b.bind(port).sync().channel();

			ch.closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	private static void init() throws Exception {
		WebServerScanner scan = new WebServerScanner(BASE_PACKAGE);
		List<String> list = scan.getFullyQualifiedClassNameList();

		for (String className : list) {
			Class<?> clazz = scan.forClassName(className);
			WebUri uri = clazz.getAnnotation(WebUri.class);

			if (uri == null)
				continue;

			Method[] methods = clazz.getMethods();

			for (Method method : methods) {
				WebUri second = method.getAnnotation(WebUri.class);
				WebMethod httpMethod = method.getAnnotation(WebMethod.class);

				if (second == null)
					continue;

				System.out.println("加载路径：" + uri.value() + second.value());

				WebServerMapping mapping = new WebServerMapping(clazz, method);
				String match = uri.value() + second.value();
				String wildcards = "^" + match.replace("*", ".*") + "$";

				// 为了提升检索速度，在服务器启动时将URL映射存放在4个Map中，此方式消耗内存较大
				if (httpMethod == null || httpMethod.method() == HttpMethod.GET) {
					if (match.indexOf("*") == -1)
						GET_MAPPING.put(match, mapping);
					else
						GET_WILDCARDS.put(wildcards, mapping);
				}

				if (httpMethod == null || httpMethod.method() == HttpMethod.POST) {
					if (match.indexOf("*") == -1)
						POST_MAPPING.put(match, mapping);
					else
						POST_WILDCARDS.put(wildcards, mapping);
				}
			}
		}
	}
}