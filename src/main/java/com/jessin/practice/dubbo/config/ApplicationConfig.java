package com.jessin.practice.dubbo.config;

import lombok.Data;

/**
 * @Author: jessin
 * @Date: 2022/1/3 5:19 下午
 */
@Data
public class ApplicationConfig {
    /**
     * 注册中心，实际可以有多个
     */
    private String registry = "127.0.0.1:2181";

    /**
     * 通信协议
     */
    private String protocol = "dubbo";
    /**
     * 通信端口
     */
    private Integer port = 20880;
}
