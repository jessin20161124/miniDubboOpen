package com.jessin.practice.dubbo.registry;

import com.jessin.practice.dubbo.utils.Pair;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 同一个zk集群同一台机器应该只建立一个共享连接
 * @Author: jessin
 * @Date: 2021/10/27 2:16 PM
 */
public class RegistryManager {

    private static Map<String, Pair<RegistryService, AtomicInteger>> map = new ConcurrentHashMap<>();

    public static RegistryService getRegistryService(String registryAddress) {
        // 双检锁，任何对象都可以作为锁
        Pair<RegistryService, AtomicInteger> nettyServerIntegerPair = map.get(registryAddress);
        if (nettyServerIntegerPair == null) {
            synchronized (map) {
                nettyServerIntegerPair = map.get(registryAddress);
                if (nettyServerIntegerPair == null) {
                    RegistryService registryService = new ZookeeperRegistryService(registryAddress);
                    nettyServerIntegerPair = new Pair<>(registryService, new AtomicInteger(0));
                    map.put(registryAddress, nettyServerIntegerPair);
                }
            }
        }
        nettyServerIntegerPair.getRight().incrementAndGet();
        return nettyServerIntegerPair.getLeft();

    }

    /**
     * 不关闭zk和server，其他地方也在用，程序停止时再关闭
     * @param registryAddress
     * @return
     */
    public static boolean remove(String registryAddress) {
        Pair<RegistryService, AtomicInteger> nettyServerIntegerPair = map.get(registryAddress);
        if (nettyServerIntegerPair != null && nettyServerIntegerPair.getRight().decrementAndGet() == 0) {
            try {
                map.remove(registryAddress);
                nettyServerIntegerPair.getLeft().close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
        return false;
    }
}
