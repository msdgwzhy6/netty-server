package netty.server.core;

import java.lang.reflect.*;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

/**
 * URL映射实体
 */
public class WebServerMapping {

	public Class<?> clazz;
	public Method method;
	public String[] names;

	public WebServerMapping() {
		super();
	}

	public WebServerMapping(Class<?> clazz, Method method) throws Exception {
		this.clazz = clazz;
		this.method = method;
		
		// 使用增强反射工具，还原出参数名，在服务器启动时预处理，可以提升运行时速度
		ClassPool cp = ClassPool.getDefault();
		cp.insertClassPath(new ClassClassPath(clazz));
		CtClass cc = cp.get(clazz.getName());
		CtMethod cm = cc.getDeclaredMethod(method.getName());

		MethodInfo methodInfo = cm.getMethodInfo();
		CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
		LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
		
		names = new String[method.getParameterTypes().length];

		if (attr != null) {
			int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;
			for (int i = 0; i < names.length; i++)
				names[i] = attr.variableName(i + pos);
		}
	}
}