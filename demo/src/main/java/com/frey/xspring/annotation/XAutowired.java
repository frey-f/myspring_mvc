package com.frey.xspring.annotation;

import java.lang.annotation.*;

/**
 * 自定义autowired注解 用于标注自动注入
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface XAutowired {
    String value() default "";
}
