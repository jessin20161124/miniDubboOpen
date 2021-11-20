package com.jessin.practice.dubbo.registry;

import com.google.common.collect.Maps;
import java.util.Map;

/**
 * @Author: jessin
 * @Date: 2021/10/27 2:16 PM
 */
public class RegistryManager {

    private static Map<String, CuratorZookeeperClient> map = Maps.newHashMap();
    public static CuratorZookeeperClient getCuratorZookeeperClient(String registry) {
        if (map.containsKey(registry)) {
            return map.get(registry);
        }
        CuratorZookeeperClient curatorZookeeperClient = new CuratorZookeeperClient(registry);
        map.put(registry, curatorZookeeperClient);
        return curatorZookeeperClient;
    }

}
