package com.jessin.practice.dubbo.spring.config;

import com.jessin.practice.dubbo.config.ApplicationConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Author: jessin
 * @Date: 2021/10/26 9:36 PM
 */
@ConfigurationProperties(MiniDubboProperties.PREFIX)
@Data
public class MiniDubboProperties extends ApplicationConfig {
    public static final String PREFIX = "mini-dubbo";

    public static final String PACKAGE_PATH = "package-path";

    private String packagePath;

    /**
     * server/client/both
     */
    private String type = "both";
}
