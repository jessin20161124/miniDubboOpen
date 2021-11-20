package com.jessin.practice.dubbo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Author: jessin
 * @Date: 2021/10/26 9:36 PM
 */
@ConfigurationProperties(MiniDubboProperties.PREFIX)
@Data
public class MiniDubboProperties {
    public static final String PREFIX = "mini-dubbo";

    public static final String PACKAGE_PATH = "package-path";

    private String packagePath;

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

    /**
     * server/client/both
     */
    private String type = "both";
}
