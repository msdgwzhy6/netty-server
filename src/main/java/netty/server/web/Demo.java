package netty.server.web;

import java.io.*;

import netty.server.annotation.*;

@WebUri
public class Demo {
	
	@WebUri("/")
	public String index(){
		return "Welcome to the netty server";
	}
	
	@WebUri("/upload")
	public String upload(File file) throws Exception {
		return file == null ? "未上传文件" : file.getPath();
	}
	
	@WebUri("/download/*")
	public String download(){
		return "bbbb";
	}
}