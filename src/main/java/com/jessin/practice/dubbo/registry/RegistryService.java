package com.jessin.practice.dubbo.registry;

import java.io.Closeable;

/**
 * @Author: jessin
 * @Date: 2022/1/1 3:21 下午
 */
public interface RegistryService extends Closeable {

    /**
     * 将路径providerPath注册上去
     * @param providerPath /miniDubbo/myGroup/com.jessin.practice.dubbo.service.DomainService/providers/192.168.1.101:20880
     */
    void register(String providerPath);

    void unregister(String providerPath);

    /**
     * 订阅prividerPath路径下的子节点变化，有变化时通过listener通知我
     * @param providerPath /miniDubbo/myGroup/com.jessin.practice.dubbo.service.DomainService/providers
     * @param childListener
     */
    void subscribe(String providerPath, ChildListener childListener);

    void unsubscribe(String providerPath);

}
