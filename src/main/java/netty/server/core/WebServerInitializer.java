package netty.server.core;

import io.netty.channel.*;
import io.netty.channel.socket.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.*;

/**
 * Web服务初始化
 */
public class WebServerInitializer extends ChannelInitializer<SocketChannel> {
	
	public void initChannel(SocketChannel ch) {
		ch.pipeline()
			.addLast(new HttpServerCodec())
			.addLast(new HttpObjectAggregator(Integer.MAX_VALUE))
			.addLast(new ChunkedWriteHandler())
			.addLast(new WebServerHandler());
	}
}