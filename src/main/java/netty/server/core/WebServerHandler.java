package netty.server.core;

import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import javassist.*;
import javassist.Modifier;
import javassist.bytecode.*;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

public class WebServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	
	private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[^-\\._]?[^<>&\\\"]*");
	
	protected void messageReceived(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		if (!request.decoderResult().isSuccess()) {
			WebServerUtil.sendError(ctx, BAD_REQUEST);
			return;
		}

		if (request.method() != GET) {
			WebServerUtil.sendError(ctx, METHOD_NOT_ALLOWED);
			return;
		}
		
		final String uri = request.uri();
		final String path = WebServerUtil.sanitizeUri(uri);
		if (path == null) {
			WebServerUtil.sendError(ctx, FORBIDDEN);
			return;
		}

		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
		response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
		
		// 解析uri
		QueryStringDecoder decoder = new QueryStringDecoder(uri);
		Map<String, List<String>> query_param = decoder.parameters();
		// 去url映射中匹配
		WebServerMapping mapping = WebServer.URL_MAPPING.get(decoder.path());

		// 如果匹配不到，转发404
		if (mapping == null) {
			WebServerUtil.sendError(ctx, NOT_FOUND);
			return;
		}
		
		// 使用增强反射工具，还原出参数名
		ClassPool cp = ClassPool.getDefault();
		cp.insertClassPath(new ClassClassPath(mapping.clazz));
		CtClass cc = cp.get(mapping.clazz.getName());
		CtMethod cm = cc.getDeclaredMethod(mapping.method.getName());

		MethodInfo methodInfo = cm.getMethodInfo();
		CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
		LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);

		// 分析入参
		Parameter[] params = mapping.method.getParameters();
		Object[] args = new Object[params.length];

		if (attr != null) {
			int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;

			for (int i = 0; i < params.length; i++) {
				String className = params[i].getType().getName();
				if (className.equals("io.netty.channel.ChannelHandlerContext")) {
					// 如果参数类型是ChannelHandlerContext
					args[i] = ctx;
				} else if (className.equals("java.lang.String")) {
					// 如果参数类型是String
					List<String> list = query_param.get(attr.variableName(i + pos));
					args[i] = WebServerUtil.listToString(list);
				} else {
					args[i] = null;
				}
			}
		}
		
		ByteBuf buffer = Unpooled.copiedBuffer((String)mapping.method.invoke(mapping.clazz.newInstance(), args), CharsetUtil.UTF_8);
		response.content().writeBytes(buffer);
		buffer.release();
		
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
		
//		File file = new File(path);
//		if (file.isHidden() || !file.exists()) {
//			WebServerUtil.sendError(ctx, NOT_FOUND);
//			return;
//		}
//
//		if (file.isDirectory()) {
//			if (uri.endsWith("/")) {
//				sendListing(ctx, file, uri);
//			} else {
//				WebServerUtil.sendRedirect(ctx, uri + '/');
//			}
//			return;
//		}
//
//		if (!file.isFile()) {
//			WebServerUtil.sendError(ctx, FORBIDDEN);
//			return;
//		}
//
//		String ifModifiedSince = (String)request.headers().get(HttpHeaderNames.IF_MODIFIED_SINCE);
//		if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
//			SimpleDateFormat dateFormatter = new SimpleDateFormat(WebServerUtil.HTTP_DATE_FORMAT, Locale.CHINA);
//			Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);
//
//			long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
//			long fileLastModifiedSeconds = file.lastModified() / 1000;
//			if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
//				WebServerUtil.sendNotModified(ctx);
//				return;
//			}
//		}
//
//		RandomAccessFile raf;
//		try {
//			raf = new RandomAccessFile(file, "r");
//		} catch (FileNotFoundException ignore) {
//			WebServerUtil.sendError(ctx, NOT_FOUND);
//			return;
//		}
//		long fileLength = raf.length();
//
//		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
//		HttpHeaderUtil.setContentLength(response, fileLength);
//		WebServerUtil.setContentTypeHeader(response, file);
//		WebServerUtil.setDateAndCacheHeaders(response, file);
//		if (HttpHeaderUtil.isKeepAlive(request)) {
//			response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
//		}
//		
//		ctx.write(response);
//		
//		ChannelFuture sendFileFuture;
//		ChannelFuture lastContentFuture;
//		if (ctx.pipeline().get(SslHandler.class) == null) {
//			sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
//			lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
//		} else {
//			sendFileFuture = ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(raf, 0, fileLength, 8192)), ctx.newProgressivePromise());
//			lastContentFuture = sendFileFuture;
//		}
//
//		sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
//			public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
//				if (total < 0) { // total unknown
//					System.err.println(future.channel() + " Transfer progress: " + progress);
//				} else {
//					System.err.println(future.channel() + " Transfer progress: " + progress + " / " + total);
//				}
//			}
//
//			public void operationComplete(ChannelProgressiveFuture future) {
//				System.err.println(future.channel() + " Transfer complete.");
//			}
//		});
//		
//		if (!HttpHeaderUtil.isKeepAlive(request)) {
//			lastContentFuture.addListener(ChannelFutureListener.CLOSE);
//		}
	}

	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		if (ctx.channel().isActive()) {
			WebServerUtil.sendError(ctx, INTERNAL_SERVER_ERROR);
		}
	}

	private static void sendListing(ChannelHandlerContext ctx, File dir, String dirPath) {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
		response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");

		StringBuilder buf = new StringBuilder()
				.append("<!DOCTYPE html>\r\n")
				.append("<html><head><meta charset='utf-8' /><title>")
				.append("Listing of: ").append(dirPath)
				.append("</title></head><body>\r\n")
				.append("<h3>Listing of: ").append(dirPath).append("</h3>\r\n")
				.append("<ul>").append("<li><a href=\"../\">..</a></li>\r\n");

		for (File f : dir.listFiles()) {
			if (f.isHidden() || !f.canRead()) {
				continue;
			}

			String name = f.getName();
			if (!ALLOWED_FILE_NAME.matcher(name).matches()) {
				continue;
			}

			buf.append("<li><a href=\"").append(name).append("\">").append(name).append("</a></li>\r\n");
		}

		buf.append("</ul></body></html>\r\n");
		ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
		response.content().writeBytes(buffer);
		buffer.release();
		
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}
}