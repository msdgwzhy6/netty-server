package netty.server.core;

import java.lang.reflect.*;

import javassist.*;
import javassist.Modifier;
import javassist.bytecode.*;

/**
 * URL映射实体
 */
public class WebServerMapping {

	public final Class<?> clazz;
	public final Method method;
	public final String[] names;

	public WebServerMapping(final Class<?> clazz, final Method method) throws Exception {
		this.clazz = clazz;
		this.method = method;
		
		// 使用增强反射工具，还原出参数名，在服务器启动时预处理，可以提升运行时速度
		final ClassPool cp = ClassPool.getDefault();
		cp.insertClassPath(new ClassClassPath(clazz));
		final CtClass cc = cp.get(clazz.getName());
		final CtMethod cm = cc.getDeclaredMethod(method.getName());

		final MethodInfo methodInfo = cm.getMethodInfo();
		final CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
		final LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
		
		names = new String[method.getParameterTypes().length];

		if (attr != null) {
			int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;
			for (int i = 0; i < names.length; i++)
				names[i] = attr.variableName(i + pos);
		}
	}
}