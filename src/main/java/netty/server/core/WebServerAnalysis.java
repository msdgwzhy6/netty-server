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
		
		// 遍历入参
		for (int i = 0; i < params.length; i++) {
			final String className = params[i].getName();
			if (className.equals("io.netty.channel.ChannelHandlerContext")) {
				// 入参类型是ChannelHandlerContext
				args[i] = ctx;
			} else if (className.equals("io.netty.handler.codec.http.FullHttpRequest")
					|| className.equals("io.netty.handler.codec.http.HttpRequest")
					|| className.equals("io.netty.handler.codec.http.HttpMessage")
					|| className.equals("io.netty.handler.codec.http.HttpObject")) {
				// 入参类型是FullHttpRequest
				args[i] = request;
			} else if (className.equals("java.lang.String")) {
				// 入参类型是String
				final List<String> list = query_param.get(mapping.names[i]);
				args[i] = WebServerUtil.listToString(list);
			} else if (className.equals("java.io.File")) {
				// 入参类型是File
				final File file = WebServerUtil.readFile(ctx, request, mapping.names[i]);
				if (file != null) {
					fileCache.add(file);
					pathCache.add(file.getPath());
				}
				args[i] = file;
			} else {
				// 入参类型无法解析
				args[i] = null;
			}
		}
		
		// 分析出参
		final Class<?> resultType = mapping.method.getReturnType();
		
		// 用于文件下载
		final HttpResponse httpResponse = new DefaultHttpResponse(HTTP_1_1, OK);
		// 用于返回结果
		final FullHttpResponse fullResponse = new DefaultFullHttpResponse(HTTP_1_1, OK);
		
		// 出参类型只需要判断3种即可:文件、void、其他，其他所有类型暂时都转做字符串处理
		switch (resultType.getName()) {
		case "java.io.File":
			// 出参类型是文件
			final File file = (File) mapping.method.invoke(mapping.clazz.newInstance(), args);

			final RandomAccessFile raf;
			try {
				raf = new RandomAccessFile(file, "r");
			} catch (FileNotFoundException ignore) {
				WebServerUtil.sendError(ctx, NOT_FOUND);
				return true;
			}

			final long fileLength = raf.length();
			
			HttpUtil.setContentLength(httpResponse, fileLength);
			WebServerUtil.setContentTypeHeader(httpResponse, file);
			WebServerUtil.setDateAndCacheHeaders(httpResponse, file);

			if (HttpUtil.isKeepAlive(request))
				httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

			ctx.write(httpResponse);
			ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
			
			ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(ChannelFutureListener.CLOSE);

			if (!HttpUtil.isKeepAlive(request))
				lastContentFuture.addListener(ChannelFutureListener.CLOSE);
			break;
		default:
			// 出参类型是文件外的其他类型
			final Object result = mapping.method.invoke(mapping.clazz.newInstance(), args);
			
			if (result != null) {
				final ByteBuf buffer = Unpooled.copiedBuffer(result.toString(), CharsetUtil.UTF_8);
				fullResponse.content().writeBytes(buffer);
				buffer.release();
			}
		case "void":
			// 没有出参
			fullResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
			ctx.writeAndFlush(fullResponse).addListener(ChannelFutureListener.CLOSE);
			break;
		}
		
		// 如果文件没有被转移，清除文件缓存
		for (int i = 0; i < fileCache.size(); i++) {
			if (fileCache.get(i).getPath().equals(pathCache.get(i)))
				fileCache.get(i).delete();
		}
		return true;
	}
}