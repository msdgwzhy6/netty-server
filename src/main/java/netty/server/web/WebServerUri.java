package netty.server.web;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Inherited //可继承
public @interface WebServerUri {
	
	String value() default "";
}