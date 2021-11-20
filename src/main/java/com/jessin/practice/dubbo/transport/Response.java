package com.jessin.practice.dubbo.transport;

import lombok.Data;

/**
 * @Author: jessin
 * @Date: 19-11-25 下午10:38
 */
@Data
public class Response {
    private long id;
    private boolean isException;
    private Object result;
}