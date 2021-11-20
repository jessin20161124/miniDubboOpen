package com.jessin.practice.dubbo.netty;

import com.google.common.collect.Maps;
import java.util.Map;

/**
 * todo 调研懒惰加载的工具类
 * @Author: jessin
 * @Date: 2021/10/27 2:23 PM
 */
public class NettyManager {
    private static Map<Integer, NettyServer> serverMap = Maps.newHashMap();
    private static Map<String, NettyClient> clientMap = Maps.newHashMap();

    public static NettyServer getNettyServer(Integer port) {
        if (serverMap.containsKey(port)) {
            return serverMap.get(port);
        }
        NettyServer nettyServer = new NettyServer(port);
        serverMap.put(port, nettyServer);
        return nettyServer;
    }

    /**
     * todo 需要计数，如果为0，可以移除，因为可能有多个接口使用同一个ipAndPort
     * @param ipPort
     * @return
     */
    public static NettyClient getNettyClient(String ipPort) {
        if (clientMap.containsKey(ipPort)) {
            return clientMap.get(ipPort);
        }
        NettyClient nettyClient = new NettyClient(ipPort);
        clientMap.put(ipPort, nettyClient);
        return nettyClient;
    }

    public static NettyClient removeNettyClient(String ipPort) {
        if (clientMap.containsKey(ipPort)) {
            return clientMap.remove(ipPort);
        }
        return null;
    }
}
