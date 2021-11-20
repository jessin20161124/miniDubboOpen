package com.jessin.practice.dubbo.config;

import com.alibaba.fastjson.parser.ParserConfig;

/**
 * https://blog.csdn.net/fly910905/article/details/81504388
 * 对于调用服务端异常时，会返回异常栈，需要开启自动化类型支持，否则反序列化会报错，需要注意
 * @Author: jessin
 * @Date: 2021/10/31 1:20 PM
 */
public class FastJsonConfig {
    static {
        ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
    }
    public static void config() {

    }
}
