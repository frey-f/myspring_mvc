package com.frey.xspring.annotation;

import java.lang.annotation.*;

/**
 * 自定义注解requestMapping
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface XRequestMapping {
    String value() default "";
}
