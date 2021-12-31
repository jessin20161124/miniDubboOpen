package com.jessin.practice.dubbo.transport;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * zk版本需要与服务端一致，支持cancel，支持注册callback，
 * volatile变量，先复制一个临时变量：Response res = response，提高性能
 * @Author: jessin
 * @Date: 19-11-25 下午10:44
 */
public class DefaultFuture {
    private static Map<Long, DefaultFuture> id2FutureMap = new ConcurrentHashMap<>();
    private static Map<Long, Request> id2RequestMap = new ConcurrentHashMap<>();


    private CountDownLatch countDownLatch = new CountDownLatch(1);

    private volatile Response response;

    public DefaultFuture(Request request) {
        id2FutureMap.put(request.getId(), this);
        id2RequestMap.put(request.getId(), request);
    }

    public Response getResponse(long waitMillis) {
        long start = System.currentTimeMillis();
        while (response == null) {
            try {
                countDownLatch.await(waitMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // 忽略中断
                e.printStackTrace();
            }
            if (System.currentTimeMillis() - start > waitMillis) {
                throw new RuntimeException("超时了");
            }
        }
        if (response.getResult() instanceof Exception) {
            throw new RuntimeException((Exception)response.getResult());
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

    /**
     * todo 需要在receive时将该id对应的entry移除，同时如果没有收到响应，应该设置超时移除。。不然map会无限扩大。
     * todo 支持cancel
     * @param response
     */
    public static void setResponse(Response response) {
        DefaultFuture defaultFuture = id2FutureMap.get(response.getId());
        if (defaultFuture != null) {
            defaultFuture.setInnerResponse(response);
        }
    }

    public static Optional<Request> getRequest(long id) {
        Request request = id2RequestMap.get(id);
        return Optional.ofNullable(request);
    }
}
