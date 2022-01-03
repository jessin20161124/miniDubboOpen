package com.jessin.practice.dubbo.spring.processor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author: jessin
 * @Date: 19-11-27 下午9:11
 */
@Target({ ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
// 必须写为runtime，否则获取不到
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Service {
    String group() default "";

    String version() default "1.0.0";

    String timeout() default "3000";
}
