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
    private boolean isEvent;

    /**
     * todo dubbo中存的是RpcResult，里面有exception/result，还需要recreate()，这里是真正的结果
     */
    private Object result;
}
