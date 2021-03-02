package com.frey.xspring.annotation;

import java.lang.annotation.*;

/**
 * 自定义 Controller注解，用于标注是Controller
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface XController {
    String value() default "";
}
