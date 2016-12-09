package netty.server.web;

import java.io.*;

import io.netty.channel.*;
import netty.server.annotation.*;

@WebUri
public class Demo {
	
	@WebUri("/")
	public String index(){
		return "Welcome to the netty server";
	}
	
	@WebUri("/upload")
	public String upload(ChannelHandlerContext ctx, String name, File file){
		System.out.println(name);
		return "aaaa";
	}
	
	@WebUri("/download/*")
	public String download(ChannelHandlerContext ctx){
		return "bbbb";
	}
}