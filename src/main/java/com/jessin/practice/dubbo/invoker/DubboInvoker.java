package com.jessin.practice.dubbo.invoker;

import com.jessin.practice.dubbo.config.InterfaceConfig;
import com.jessin.practice.dubbo.netty.NettyClient;
import com.jessin.practice.dubbo.netty.NettyManager;
import com.jessin.practice.dubbo.transport.DefaultFuture;
import com.jessin.practice.dubbo.transport.Request;
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
        nettyClient = NettyManager.getNettyClient(ipAndPort);
    }

    /**
     * 抽取接口，可以有http invoker
     * @param rpcInvocation
     * @return
     */
    public Object invoke(RpcInvocation rpcInvocation) {
        log.info("向服务：{}发起请求", ipAndPort);
        Request request = new Request();
        // TODO 接口名待从url获取，版本待从url获取
        request.setRpcInvocation(rpcInvocation);
        nettyClient.send(request);
        DefaultFuture defaultFuture = new DefaultFuture(request);

        // 阻塞等待结果...
        // TODO 超时时间待从url获取
        return defaultFuture.getResponse(Long.parseLong(interfaceConfig.getTimeout()));
    }

    public void destroy() {
        NettyClient nettyClient = NettyManager.removeNettyClient(ipAndPort);
        if (nettyClient != null) {
            nettyClient.close();
        }
    }
}
