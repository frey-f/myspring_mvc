package com.frey.xspring.annotation;

import java.lang.annotation.*;

/**
 * 自定义注解 Service
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface XService {
    String value() default "";
}
