package netty.server.web;

import java.io.*;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import netty.server.annotation.*;

@WebUri
public class Demo {
	
	@WebUri("/")
	public String index(){
		return "Welcome to the netty server";
	}
	
	@WebUri("/upload")
	public String upload(ChannelHandlerContext ctx, String name, HttpRequest request){
		System.out.println(request.uri());
		return "aaaa";
	}
	
	@WebUri("/download/*")
	public String download(ChannelHandlerContext ctx){
		return "bbbb";
	}
}