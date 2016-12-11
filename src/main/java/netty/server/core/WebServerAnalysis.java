package netty.server.core;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.*;

import java.io.*;
import java.util.*;

/**
 * URL解析类
 */
class WebServerAnalysis {

	static boolean analysis(final ChannelHandlerContext ctx, final FullHttpRequest request) throws Exception {
		final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
		response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");

		// 解析uri
		final QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
		final Map<String, List<String>> query_param = decoder.parameters();
		// 去url映射中匹配
		final WebServerMapping mapping = WebServerMapping.get(request, decoder.path());

		if (mapping == null)
			return false;

		// 分析入参
		final Class<?>[] params = mapping.method.getParameterTypes();
		final Object[] args = new Object[params.length];
		
		// 文件缓存
		final List<File> fileCache = new ArrayList<File>();
		final List<String> pathCache = new ArrayList<String>();
		
		for (int i = 0; i < params.length; i++) {
			final String className = params[i].getName();
			if (className.equals("io.netty.channel.ChannelHandlerContext")) {
				// 如果参数类型是ChannelHandlerContext
				args[i] = ctx;
			} else if (className.equals("io.netty.handler.codec.http.FullHttpRequest")
					|| className.equals("io.netty.handler.codec.http.HttpRequest")
					|| className.equals("io.netty.handler.codec.http.HttpMessage")
					|| className.equals("io.netty.handler.codec.http.HttpObject")) {
				// 如果参数类型是FullHttpRequest
				args[i] = request;
			} else if (className.equals("java.lang.String")) {
				// 如果参数类型是String
				final List<String> list = query_param.get(mapping.names[i]);
				args[i] = WebServerUtil.listToString(list);
			} else if (className.equals("java.io.File")) {
				// 如果参数类型是File
				final File file = WebServerUtil.readFile(ctx, request, mapping.names[i]);
				if (file != null) {
					fileCache.add(file);
					pathCache.add(file.getPath());
				}
				args[i] = file;
			} else {
				args[i] = null;
			}
		}
		
		// 分析出参
		final Class<?> resultType = mapping.method.getReturnType();
		final String result;

		if (resultType.getName().equals("java.lang.String")) {
			result = (String) mapping.method.invoke(mapping.clazz.newInstance(), args);
		} else {
			result = null;
		}

		if (result != null) {
			final ByteBuf buffer = Unpooled.copiedBuffer(result, CharsetUtil.UTF_8);
			response.content().writeBytes(buffer);
			buffer.release();
		}
		
		// 如果文件没有被转移，清除文件缓存
		for (int i = 0; i < fileCache.size(); i++) {
			if (fileCache.get(i).getPath().equals(pathCache.get(i)))
				fileCache.get(i).delete();
		}
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
		return true;
	}
}