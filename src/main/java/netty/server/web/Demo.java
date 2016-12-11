package netty.server.web;

import java.io.*;

import netty.server.annotation.*;
import netty.server.annotation.type.*;

@WebUri
public class Demo {
	
	@WebUri("/")
	public String index(){
		return "Welcome to the netty server";
	}
	
	@WebUri("/upload")
	@WebMethod(method = HttpMethod.POST)
	public String upload(File file) throws Exception {
		return file == null ? "未上传文件" : file.getPath();
	}
	
	@WebUri("/download/*")
	@WebMethod(method = HttpMethod.GET)
	public String download(){
		return "bbbb";
	}
}