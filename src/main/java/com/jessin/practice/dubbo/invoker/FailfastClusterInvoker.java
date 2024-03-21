package com.jessin.practice.dubbo.invoker;

import com.jessin.practice.dubbo.registry.RegistryDirectory;

import java.util.List;

/**
 * 集群容灾策略，jdk代理直接使用
 * @Author: jessin
 * @Date: 19-11-25 下午10:13
 */
public class FailfastClusterInvoker {

    private RegistryDirectory registryDirectory;

    public FailfastClusterInvoker(RegistryDirectory registryDirectory) {
        this.registryDirectory = registryDirectory;
    }

    /**
     * 这里从RegistryDirectory获取invoker列表，选择一台进行调用，底层调用netty连接 抽取接口
     * todo 先路由实现，例如同一个泳道标、同机房发现，然后再负载均衡算法，随机/轮询/加权
     */
    public Object invoke(RpcInvocation rpcInvocation) {
        List<DubboInvoker> dubboInvokerList = registryDirectory.getInvokerList();
        if (dubboInvokerList.size() == 0) {
            throw new RuntimeException("no provider ");
        }
        return dubboInvokerList.get(0).invoke(rpcInvocation);
    }
}