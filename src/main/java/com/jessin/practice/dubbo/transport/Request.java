package com.jessin.practice.dubbo.transport;

import com.jessin.practice.dubbo.invoker.RpcInvocation;
import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author: jessin
 * @Date: 19-11-25 下午10:34
 */
@Data
public class Request {
    private static AtomicLong idGenerator = new AtomicLong();
    /**
     * 每个请求生成唯一的id
     */
    private long id = idGenerator.incrementAndGet();

    /**
     * 数据
     */
    private RpcInvocation rpcInvocation;
}
