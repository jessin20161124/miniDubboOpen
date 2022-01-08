package com.jessin.practice.dubbo.transport;

import com.google.common.collect.Lists;
import com.jessin.practice.dubbo.exception.DubboException;
import com.jessin.practice.dubbo.utils.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * zk版本需要与服务端一致，支持cancel，支持注册callback，
 * volatile变量，先复制一个临时变量：Response res = response，提高性能
 * @Author: jessin
 * @Date: 19-11-25 下午10:44
 */
@Slf4j
public class DefaultFuture {

    private static final RuntimeException TIMEOUT_EXCEPTION = new RuntimeException("超时了");

    private static Map<Long, DefaultFuture> id2FutureMap = new ConcurrentHashMap<>();

    private static ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    private long timeout;

    private volatile Response response;

    private Request request;

    private List<Callback> callbackList = Lists.newArrayList();

    public DefaultFuture(Request request, long timeout) {
        this.request = request;
        this.timeout = timeout;
        id2FutureMap.put(request.getId(), this);
        // 如果没有收到响应，应该设置超时移除。不然map会无限扩大
        scheduledExecutorService.schedule(() -> {
            Response response = new Response();
            response.setId(request.getId());
            response.setException(true);
            response.setResult(StringUtils.toString(TIMEOUT_EXCEPTION));
            if (id2FutureMap.containsKey(request.getId())) {
                log.warn("客户端定时器检测到超时没有回复，自动抛出异常：{}", request);
            }
            try {
                setResponse(response);
            } catch (Exception e) {
                log.error("auto error", e);
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }

    public Request getRequest() {
        return request;
    }

    /**
     * 因为netty是异步的，这里需要循环等待服务端返回数据，业务线程等待netty io线程设置
     * @param waitMillis
     * @return
     */
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
                throw TIMEOUT_EXCEPTION;
            }
        }
        if (response.isException()) {
            throw new DubboException((String)response.getResult());
        }
        return response;
    }

    /**
     * 只能通过下面静态方法修改
     * @param response
     */
    private void setInnerResponse(Response response) {
        this.response = response;
        countDownLatch.countDown();
        callbackList.forEach((callback -> {
            if (response.isException()) {
                callback.finish(null, new DubboException((String)response.getResult()));
            } else {
                callback.finish(response.getResult(), null);
            }
        }));
    }

    /**
     * 需要在receive时将该id对应的entry移除
     * @param response
     */
    public static void setResponse(Response response) {
        DefaultFuture defaultFuture = id2FutureMap.remove(response.getId());
        if (defaultFuture != null) {
            defaultFuture.setInnerResponse(response);
        }
    }

    public static Optional<Request> getRequest(long id) {
        DefaultFuture defaultFuture = id2FutureMap.get(id);
        return defaultFuture != null ? Optional.ofNullable(defaultFuture.getRequest()) : Optional.empty();
    }

    public void addCallback(Callback callback) {
        callbackList.add(callback);
    }

    public interface Callback {

        /**
         * 当有结果时，调用这个回调函数
         * @param result
         * @param exception
         */
        void finish(Object result, Exception exception);
    }
}
