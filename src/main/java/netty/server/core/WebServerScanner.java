package netty.server.core;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;

/**
 * 扫描包下所有类
 */
public class WebServerScanner {
	
	private String basePackage;
	private ClassLoader cl;

	public WebServerScanner(String basePackage) {
		this.basePackage = basePackage;
		this.cl = getClass().getClassLoader();
	}

	public WebServerScanner(String basePackage, ClassLoader cl) {
		this.basePackage = basePackage;
		this.cl = cl;
	}
	
	public List<String> getFullyQualifiedClassNameList() throws IOException {
        System.out.println("开始扫描包" + basePackage + "下的所有类");
 
        return doScan(basePackage, new ArrayList<String>());
    }
	
	public Class<?> forClassName(String name) throws Exception {
		URLClassLoader loader = null;
		try {
			String splashPath = dotToSplash(basePackage);

			URL url = cl.getResource(splashPath);
			String filePath = getRootPath(url);

			if (isJarFile(filePath)) {
				URL[] urls = new URL[] { url };
				loader = new URLClassLoader(urls);

				return loader.loadClass(name);
			} else {
				return Class.forName(name);
			}
		} finally {
			if (loader != null)
				loader.close();
		}
	}

	private List<String> doScan(String basePackage, List<String> nameList) throws IOException {
		String splashPath = dotToSplash(basePackage);

		URL url = cl.getResource(splashPath);
		String filePath = getRootPath(url);

		List<String> names = null;
		if (isJarFile(filePath)) {
			System.out.println(filePath + "是一个JAR包");

			names = readFromJarFile(filePath, splashPath);
		} else {
			System.out.println(filePath + "是一个目录");

			names = readFromDirectory(filePath);
		}
		
		for (String name : names)
			if (isClassFile(name))
				nameList.add(isJarFile(filePath) ? splashToDot(name) : toFullyQualifiedName(name, basePackage));
			else
				doScan(basePackage + "." + name, nameList);

		for (String n : nameList)
			System.out.println("找到" + n);

		return nameList;
	}
	
	private String toFullyQualifiedName(String shortName, String basePackage) {
		StringBuilder sb = new StringBuilder(basePackage);
		sb.append('.');
		sb.append(trimExtension(shortName));

		return sb.toString();
	}

	private List<String> readFromJarFile(String jarPath, String splashedPackageName) throws IOException {
		System.out.println("从JAR包中读取类:" + jarPath);

		List<String> nameList = new ArrayList<String>();

		JarInputStream jarIn = null;
		try {
			jarIn = new JarInputStream(new FileInputStream(jarPath));
			JarEntry entry = null;

			while ((entry = jarIn.getNextJarEntry()) != null) {
				String name = entry.getName();
				if (name.startsWith(splashedPackageName) && isClassFile(name))
					nameList.add(name);
			}
		} finally {
			if (jarIn != null)
				jarIn.close();
		}

		return nameList;
	}

	private List<String> readFromDirectory(String path) {
		File file = new File(path);
		String[] names = file.list();

		return null == names ? null : Arrays.asList(names);
	}

	private boolean isClassFile(String name) {
		return name.endsWith(".class");
	}

	private boolean isJarFile(String name) {
		return name.endsWith(".jar");
	}

	public static String getRootPath(URL url) {
		String fileUrl = url.getFile();
		int pos = fileUrl.indexOf('!');

		return -1 == pos ? fileUrl : fileUrl.substring(5, pos);
	}

	public static String dotToSplash(String name) {
		return name.replaceAll("\\.", "/");
	}

	public static String splashToDot(String name) {
		return trimExtension(name).replaceAll("/", "\\.");
	}

	public static String trimExtension(String name) {
		int pos = name.indexOf('.');
		return -1 == pos ? name : name.substring(0, pos);
	}
}