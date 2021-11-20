package com.jessin.practice.dubbo.transport;

import com.alibaba.fastjson.JSON;
import com.jessin.practice.dubbo.exception.DubboException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * zk版本需要与服务端一致
 * @Author: jessin
 * @Date: 19-11-25 下午10:44
 */
public class DefaultFuture {
    private static Map<Long, DefaultFuture> id2FutureMap = new ConcurrentHashMap<>();

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    private volatile Response response;

    public DefaultFuture(Request request) {
        id2FutureMap.put(request.getId(), this);
    }

    public Response getResponse(long waitMillis) {
        long start = System.currentTimeMillis();
        while (response == null) {
            try {
                countDownLatch.await(waitMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (System.currentTimeMillis() - start > waitMillis) {
                throw new RuntimeException("超时了");
            }
        }
        if (response.isException()) {
            String exceptionStr = JSON.toJSONString(response.getResult());
            DubboException dubboException = JSON.parseObject(exceptionStr, DubboException.class);
            throw dubboException;
        }
        return response;
    }

    /**
     * 只能通过下面静态方法修改
     * @param resonse
     */
    private void setInnerResponse(Response resonse) {
        this.response = resonse;
        countDownLatch.countDown();
    }

    public static void setResponse(Response response) {
        DefaultFuture defaultFuture = id2FutureMap.get(response.getId());
        if (defaultFuture != null) {
            defaultFuture.setInnerResponse(response);
        }
    }
}
