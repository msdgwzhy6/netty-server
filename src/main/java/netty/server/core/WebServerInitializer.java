package netty.server.core;

import io.netty.channel.*;
import io.netty.channel.socket.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.*;

/**
 * Web服务初始化
 */
class WebServerInitializer extends ChannelInitializer<SocketChannel> {
	
	protected void initChannel(final SocketChannel ch) {
		ch.pipeline()
			.addLast(new HttpServerCodec())
			.addLast(new HttpObjectAggregator(Integer.MAX_VALUE))
			.addLast(new ChunkedWriteHandler())
			.addLast(new WebServerHandler());
	}
}