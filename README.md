# 项目介绍 #

### 项目环境 ###
* Jdk1.7及以上版本

### 项目启动 ###
* 在IDE中直接运行App.java
* 使用Maven的`clean package`命令打包，然后使用`java -jar netty-server.jar`运行

### 项目配置 ###
使用netty.server.core.WebServer.run()启动服务
* server.properties配置了port时，服务端口为port
* server.properties没有配置port时，服务端口为80

使用netty.server.core.WebServer.run(int port)启动服务
* 服务端口为入参port

项目根路径
* server.properties必须配置basePackage，该包下所有类及子类会被自动扫描

### 项目规范 ###
可参照`netty.server.web.Demo`
```java
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
	String index(){
		return "Welcome to the netty server";
	}

	/**
	 * 请求路径为:"/upload"
	 *
	 * (@WebMethod注解为HttpMethod.POST时只接收POST请求)
	 *
	 * 入参类型为File，接收对应参数名的文件
	 * 例如入参对象名为file，接收<input type="file" name="file" />，如果name为file的文件有多个，则接收第一个
	 *
	 * 除此之外，入参还支持String和ChannelHandlerContext类型
	 */
	@WebUri("/upload")
	@WebMethod(method = POST)
	String upload(File file) throws Exception {
		// 如果文件存储目录不存在，创建该目录
		File directory = new File(home);
		if (!directory.exists())
			directory.mkdir();

		// 将入参File转移就可以永久保留，否则改方法结束后清除缓存
		File newFile = new File(home + file.getName());
		file.renameTo(newFile);

		// 将文件名输出到页面，如果文件不存在，提示"未上传文件"
		return file == null ? "未上传文件" : newFile.getName();
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
}
```

### 未完成 ###
* 模版引擎(暂不打算开发，可作为独立项目)
* 转发和重定向(本项目用于文件/接口服务器，暂不打算开发)
