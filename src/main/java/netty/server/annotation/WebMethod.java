package netty.server.annotation;

import java.lang.annotation.*;

import netty.server.annotation.type.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface WebMethod {

	HttpMethod method();
}