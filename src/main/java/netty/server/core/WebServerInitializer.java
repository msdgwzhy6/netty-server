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
		ChannelPipeline pipeline = ch.pipeline()
				.addLast(new HttpServerCodec())
				.addLast(new HttpObjectAggregator(65536))
				.addLast(new ChunkedWriteHandler())
				.addLast(new WebServerHandler());
	}
}