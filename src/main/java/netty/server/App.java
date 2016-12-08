package netty.server;

import netty.server.core.WebServer;
import netty.server.web.*;

public class App {

	public static void main(String[] args) throws Exception {
		WebServer.run(8080); //启动Web服务器
	}
}