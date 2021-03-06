package netty.server.web;

import io.netty.handler.codec.http.*;

import java.io.*;

import net.sf.json.*;
import netty.server.annotation.*;
import static netty.server.annotation.type.HttpMethod.*;

/**
 * 相当于J2EE中的Servlet，使用@WebUri注解的类才会被加载
 * 
 * 类一定要定义为public，方法不需要
 * 
 * @author vermisse
 */
@WebUri
public class Demo {
	
	private final String home = "/file/";
	
	/**
	 * 请求根路径
	 * 
	 * (不使用@WebMethod注解时GET请求和POST请求都可以访问)
	 * 
	 * 出参类型为String，将该字符串输出到页面
	 */
	@WebUri("/")
	String index() {
		// 如果文件存储目录已存在，以JSON形式返回文件名
		File directory = new File(home);
		if (directory.exists())
			return JSONArray.fromObject(directory.list()).toString();

		// 否则提示没有文件
		return "没有文件";
	}
	
	/**
	 * 请求路径为:"/upload"
	 * 
	 * (@WebMethod注解为HttpMethod.POST时只接收POST请求)
	 * 
	 * 入参类型为File，接收对应参数名的文件，字符集必须是UTF-8格式，否则文件名不能为中文
	 * 例如入参对象名为file，接收<input type="file" name="file" />，如果name为file的文件有多个，则接收第一个
	 */
	@WebUri("/upload")
	@WebMethod(method = POST)
	String upload(File file) {
		// 文件不存在，提示"未上传文件"
		if (file == null)
			return "未上传文件";

		// 如果文件存储目录不存在，创建该目录
		File directory = new File(home);
		if (!directory.exists())
			directory.mkdir();

		// 将入参File转移(或重命名)就可以永久保留，否则该方法结束后清除缓存
		File newFile = new File(home + file.getName());
		file.renameTo(newFile);

		// 将文件名输出到页面
		return file.getName();
	}
	
	/**
	 * 请求路径为:"/download/*"，*为通配符
	 * 
	 * (@WebMethod注解为HttpMethod.GET时只接收GET请求)
	 * 
	 * 入参类型为HttpRequest，获取本次请求的Request对象(Netty类型，与J2EE不一致)
	 * 出参类型为File，下载该文件
	 */
	@WebUri("/download/*")
	@WebMethod(method = GET)
	File download(HttpRequest request) {
		// 解析uri，也就是url去掉协议、域名/IP、端口的部分
		QueryStringDecoder decoder = new QueryStringDecoder(request.uri());

		// 在目录中搜索该文件并下载，不用担心文件不存在，如果文件不存在返回404错误
		return new File(home + decoder.path().substring(10));
	}

	/**
	 * 按名称删除文件
	 */
	@WebUri("/remove")
	String remove(final String name) {
		File directory = new File(home);
		if (!directory.exists())
			return "文件不存在";

		File[] files = directory.listFiles(new FilenameFilter() {
			// 如果文件存储目录已存在，过滤出该文件
			public boolean accept(File dir, String filterName) {
				return name.equals(filterName);
			}
		});

		if (files == null || files.length == 0)
			return "文件不存在";

		// 返回删除结果
		return files[0].delete() ? "删除成功" : "删除失败";
	}

	/**
	 * 清除所有文件
	 */
	@WebUri("/clear")
	String clear() {
		// 如果文件存储目录已存在，删除文件及文件夹
		File directory = new File(home);
		if (directory.exists()) {
			for (File file : directory.listFiles())
				file.delete();
			
			directory.delete();
		}

		return "清理成功";
	}
}