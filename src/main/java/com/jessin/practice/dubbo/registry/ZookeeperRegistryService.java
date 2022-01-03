package com.jessin.practice.dubbo.registry;

import java.io.IOException;

/**
 * @Author: jessin
 * @Date: 2022/1/1 4:13 下午
 */
public class ZookeeperRegistryService implements RegistryService {

    private CuratorZookeeperClient curatorZookeeperClient;

    public ZookeeperRegistryService(String registryAddress) {
        curatorZookeeperClient = new CuratorZookeeperClient(registryAddress);
    }

    @Override
    public void register(String providerPath) {
        // 临时节点
        curatorZookeeperClient.create(providerPath, true);
    }

    @Override
    public void unregister(String providerPath) {
        curatorZookeeperClient.delete(providerPath);
    }

    @Override
    public void subscribe(String providerPath, ChildListener childListener) {
        // 添加watcher
        curatorZookeeperClient.addTargetChildListener(providerPath, childListener);
    }

    @Override
    public void unsubscribe(String providerPath) {
        curatorZookeeperClient.removeTargetChildListener(providerPath);
    }

    @Override
    public void close() throws IOException {
        curatorZookeeperClient.doClose();
    }
}
