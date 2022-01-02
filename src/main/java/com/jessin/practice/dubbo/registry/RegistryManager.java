package com.jessin.practice.dubbo.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: jessin
 * @Date: 2021/10/27 2:16 PM
 */
public class RegistryManager {

    private static final Map<String, RegistryService> map = new ConcurrentHashMap<>();
    public static RegistryService getRegistryService(String registryAddress) {
        // 双检锁，任何对象都可以作为锁
        if (map.containsKey(registryAddress)) {
            return map.get(registryAddress);
        }
        synchronized (map) {
            if (map.containsKey(registryAddress)) {
                return map.get(registryAddress);
            }
            RegistryService registryService = new ZookeeperRegistryService(registryAddress);
            map.put(registryAddress, registryService);
            return registryService;
        }
    }
}
