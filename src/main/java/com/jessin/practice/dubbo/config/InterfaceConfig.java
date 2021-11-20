package com.jessin.practice.dubbo.config;

import lombok.Data;

/**
 * @Author: jessin
 * @Date: 2021/10/27 11:50 AM
 */
@Data
public class InterfaceConfig {

    private String group;

    private String version;

    private String timeout;

    private String failStrategy;

    private String retryCount;
}
