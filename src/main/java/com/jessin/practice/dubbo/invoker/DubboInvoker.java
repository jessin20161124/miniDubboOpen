package com.jessin.practice.dubbo.invoker;

import com.jessin.practice.dubbo.config.InterfaceConfig;
import com.jessin.practice.dubbo.netty.NettyClient;
import com.jessin.practice.dubbo.netty.NettyManager;
import com.jessin.practice.dubbo.transport.DefaultFuture;
import com.jessin.practice.dubbo.transport.Request;
import com.jessin.practice.dubbo.transport.Response;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

/**
 * 给cluster使用，底层封装netty连接
 * @Author: jessin
 * @Date: 19-11-25 下午10:04
 */
@Slf4j
public class DubboInvoker {
    private NettyClient nettyClient;
    /**
     * 消费端url
     */
    private String url;

    private InterfaceConfig interfaceConfig;

    private String ipAndPort;

    public DubboInvoker(String ipAndPort, InterfaceConfig interfaceConfig) {
        this.ipAndPort = ipAndPort;
        this.interfaceConfig = interfaceConfig;
        // 对于同一个机器可以共享，可以使用缓存
        // todo 支持使用连接池，同一个server机器可以建立多个连接，构成一个池子，随机使用，类似于jdbc连接池、http连接池、redis连接池
        nettyClient = NettyManager.getNettyClient(ipAndPort);
    }

    /**
     * todo 抽取接口，可以有http invoker，filter实现
     * 接口名待从url获取，版本待从url获取，超时时间待从url获取
     * @param rpcInvocation
     * @return
     */
    public Object invoke(RpcInvocation rpcInvocation) {
        log.info("向服务：{}发起请求", ipAndPort);
        Request request = new Request();
        request.setRpcInvocation(rpcInvocation);
        nettyClient.send(request);
        long timeout = Long.parseLong(interfaceConfig.getTimeout());
        DefaultFuture defaultFuture = new DefaultFuture(request, timeout);

        // 客户端返回一个假的CompletableFuture，当DefaultFuture有值时再传递过去，要求服务端只传递值，而不是CompletableFuture
        if (CompletableFuture.class.isAssignableFrom(rpcInvocation.getMethod().getReturnType())) {
            // 返回结果泛型类型由序列化保证，在序列化时，需要带上该实例的类型，然后在客户端反序列化才能成功
            CompletableFuture ret = new CompletableFuture();
            defaultFuture.addCallback((result, exception) -> {
                if (exception != null) {
                    ret.completeExceptionally(exception);
                } else {
                    ret.complete(result);
                }
            });
            // todo 外层是response，所以这里需要适配下
            Response response = new Response();
            response.setId(request.getId());
            response.setResult(ret);
            return response;
        }
        // 阻塞等待结果
        return defaultFuture.getResponse(timeout);
    }

    public boolean isAvailable() {
        return nettyClient.isConnected();
    }

    public String getIpAndPort() {
        return ipAndPort;
    }

    public void destroy() {
        // 引用计数移除
        NettyManager.removeNettyClient(ipAndPort);
    }
}
