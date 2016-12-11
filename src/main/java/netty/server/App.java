package netty.server;

import netty.server.core.*;

public class App {

	public static void main(String[] args) throws Exception {
		String port = WebServerUtil.getProperties("config.properties", "port");
		WebServer.run(port == null ? 8080 : Integer.valueOf(port)); //启动Web服务器
	}
}