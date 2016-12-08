package netty.server.web;

import io.netty.channel.*;
import netty.server.annotation.*;

@WebUri
public class Demo {
	
	@WebUri("/")
	public String index(){
		return "Welcome to the netty server";
	}
	
	@WebUri("/upload")
	public String upload(ChannelHandlerContext ctx, String name){
		System.out.println(name);
		return "aaaa";
	}
	
	@WebUri("/download/*")
	public String download(ChannelHandlerContext ctx){
		return "bbbb";
	}
}