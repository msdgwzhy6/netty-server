package netty.server.core;

import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.*;

import java.io.*;
import java.util.regex.*;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * Http请求统一处理
 */
public class WebServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	
	private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[^-\\._]?[^<>&\\\"]*");

	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		if (!request.decoderResult().isSuccess()) {
			WebServerUtil.sendError(ctx, BAD_REQUEST);
			return;
		}

//		if (request.method() != GET) {
//			WebServerUtil.sendError(ctx, METHOD_NOT_ALLOWED);
//			return;
//		}
		
		//解析url，如果配置了并成功执行就不再执行后续操作
		if (WebServerAnalysis.analysis(ctx, request))
			return;
		
		WebServerUtil.sendError(ctx, NOT_FOUND);
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
		if (ctx.channel().isActive())
			WebServerUtil.sendError(ctx, INTERNAL_SERVER_ERROR);
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