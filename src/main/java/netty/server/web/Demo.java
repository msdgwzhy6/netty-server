package netty.server.web;

import io.netty.handler.codec.http.*;

import java.io.*;

import netty.server.annotation.*;
import static netty.server.annotation.type.HttpMethod.*;

/**
 * 相当于J2EE中的Servlet，使用@WebUri注解的类才会被URL匹配
 * 
 * @author vermisse
 */
@WebUri
public class Demo {
	
	/**
	 * 请求根路径
	 * 
	 * (不使用@WebMethod注解时GET请求和POST请求都可以访问)
	 */
	@WebUri("/")
	public String index(){
		return "Welcome to the netty server";
	}
	
	/**
	 * 请求路径为:"/upload"
	 * 
	 * (@WebMethod注解为HttpMethod.POST时只接收POST请求)
	 */
	@WebUri("/upload")
	@WebMethod(method = POST)
	public String upload(File file) throws Exception {
		File directory = new File("D:\\catch\\");
		if(!directory.exists())
			directory.mkdir();
		
		File newFile = new File("D:\\catch\\" + file.getName());
		file.renameTo(newFile);
		return file == null ? "未上传文件" : newFile.getPath();
	}
	
	/**
	 * 请求路径为:"/download/*"，*为通配符
	 * 
	 * (@WebMethod注解为HttpMethod.GET时只接收GET请求)
	 */
	@WebUri("/download/*")
	@WebMethod(method = GET)
	public File download(HttpRequest request){
		QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
		return new File("D:\\catch\\" + decoder.path().substring(10));
	}
}