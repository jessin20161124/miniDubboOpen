package com.jessin.practice.dubbo.registry;

import java.io.IOException;
import java.util.List;

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
    public void subscribe(String providerPath, ChildListener childListener) {
        List<String> children = curatorZookeeperClient.addTargetChildListener(providerPath, childListener);
        // 第一次，需要手动通知
        childListener.childChanged(providerPath, children);
    }

    @Override
    public void close() throws IOException {
        curatorZookeeperClient.doClose();
    }
}
