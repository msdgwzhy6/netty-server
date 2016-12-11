package netty.server.core;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;

import java.util.regex.*;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

/**
 * Http请求统一处理
 */
class WebServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	
	private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[^-\\._]?[^<>&\\\"]*");

	protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest request) throws Exception {
		if (!request.decoderResult().isSuccess()) {
			WebServerUtil.sendError(ctx, BAD_REQUEST);
			return;
		}
		
		//解析url，如果配置了并成功执行就不再执行后续操作
		if (WebServerAnalysis.analysis(ctx, request))
			return;
		
		WebServerUtil.sendError(ctx, NOT_FOUND);
	}

	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		if (ctx.channel().isActive())
			WebServerUtil.sendError(ctx, INTERNAL_SERVER_ERROR);
	}
}