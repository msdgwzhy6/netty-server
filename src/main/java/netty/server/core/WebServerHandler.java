package netty.server.core;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;

import java.util.regex.*;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

/**
 * Http请求统一处理
 */
public class WebServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	
	private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[^-\\._]?[^<>&\\\"]*");

	protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest request) throws Exception {
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
}