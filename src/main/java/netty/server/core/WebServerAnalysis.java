package netty.server.core;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.*;

import java.util.*;

/**
 * URL解析类
 */
public class WebServerAnalysis {

	public static boolean analysis(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
		response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");

		// 解析uri
		QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
		Map<String, List<String>> query_param = decoder.parameters();
		// 去url映射中匹配
		WebServerMapping mapping = WebServer.URL_MAPPING.get(decoder.path());

		if (mapping == null)
			return false;

		// 分析入参
		Class<?>[] params = mapping.method.getParameterTypes();
		Object[] args = new Object[params.length];

		for (int i = 0; i < params.length; i++) {
			String className = params[i].getName();
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
				List<String> list = query_param.get(mapping.names[i]);
				args[i] = WebServerUtil.listToString(list);
			} else if (className.equals("java.io.File")) {
				// 如果参数类型是File
				// 参考http://netty.io/4.1/xref/io/netty/example/http/upload/package-summary.html
				args[i] = null;
			} else {
				args[i] = null;
			}
		}

		ByteBuf buffer = Unpooled.copiedBuffer((String) mapping.method.invoke(mapping.clazz.newInstance(), args), CharsetUtil.UTF_8);
		response.content().writeBytes(buffer);
		buffer.release();

		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
		return true;
	}
}