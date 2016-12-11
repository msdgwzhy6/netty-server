package netty.server.core;

import java.lang.reflect.*;
import java.util.*;

import io.netty.bootstrap.*;
import io.netty.channel.*;
import io.netty.channel.nio.*;
import io.netty.channel.socket.nio.*;
import io.netty.handler.logging.*;

import netty.server.annotation.*;

/**
 * Web服务启动类
 */
public final class WebServer {
	
	private static final String BASE_PACKAGE = WebServerUtil.getProperties("config.properties", "basePackage");
	public static final Map<String, WebServerMapping> URL_MAPPING = new HashMap<String, WebServerMapping>();
	
	public static void run(int port) throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		
		try {
			init();
			
			ServerBootstrap b = new ServerBootstrap()
				.group(bossGroup, workerGroup)
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
			WebUri uri = clazz.getAnnotation(WebUri.class);
			
			if (uri == null)
				continue;

			Method[] methods = clazz.getMethods();

			for (Method method : methods) {
				WebUri second = method.getAnnotation(WebUri.class);

				if (second == null)
					continue;
				
				System.out.println("加载路径：" + uri.value() + second.value());
				
				URL_MAPPING.put(uri.value() + second.value(), new WebServerMapping(clazz, method));
			}
		}
	}
}