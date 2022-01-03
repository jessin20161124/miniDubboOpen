package com.jessin.practice.dubbo.spring.processor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 消费端注解
 * @Author: jessin
 * @Date: 19-11-25 下午9:48
 */
@Target({ ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
// 必须写为runtime，否则获取不到
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Reference {

    String group() default "";

    String version() default "1.0.0";

    String timeout() default "3000";

    String failStrategy() default "failover";

    String retryCount() default "3";
}
