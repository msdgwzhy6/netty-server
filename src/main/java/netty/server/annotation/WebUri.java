package netty.server.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Inherited //可继承
public @interface WebUri {
	
	String value() default "";
}