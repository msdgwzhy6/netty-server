package netty.server.web;

import java.lang.reflect.*;
import java.util.*;

import io.netty.bootstrap.*;
import io.netty.channel.*;
import io.netty.channel.nio.*;
import io.netty.channel.socket.nio.*;
import io.netty.handler.logging.*;

/**
 * 基于Netty的Web服务器
 */
public final class WebServer {
	
	private static final String BASE_PACKAGE = "netty.server.web.controller";
	public static final Map<String, Method> URL_MAPPING = new HashMap<String, Method>();
	
	public static void run(int port) throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		
		try {
			init();
			
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.handler(new LoggingHandler(LogLevel.INFO))
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
			WebServerUri uri = clazz.getAnnotation(WebServerUri.class);
			
			if (uri == null)
				continue;

			Method[] methods = clazz.getMethods();

			for (Method method : methods) {
				WebServerUri second = method.getAnnotation(WebServerUri.class);

				if (second == null)
					continue;
				
				System.out.println("拦截路径：" + uri.value() + second.value());
				
				URL_MAPPING.put(uri.value() + second.value(), method);
			}
		}
	}
}