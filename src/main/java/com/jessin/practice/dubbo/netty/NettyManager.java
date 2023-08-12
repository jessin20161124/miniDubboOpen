package com.jessin.practice.dubbo.netty;

import com.jessin.practice.dubbo.utils.Pair;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 对于同一个机器ip:port可以共享，可以使用缓存
 * 引用计数法销毁
 *
 * @Author: jessin
 * @Date: 2021/10/27 2:23 PM
 */
public class NettyManager {
    private static final Map<Integer, Pair<NettyServer, AtomicInteger>> serverMap = new ConcurrentHashMap<>();
    private static final Map<String, Pair<NettyClient, AtomicInteger>> clientMap = new ConcurrentHashMap<>();

    private NettyManager() {

    }

    public static NettyServer getNettyServer(Integer port) {
        // 双检锁，任何对象都可以作为锁
        Pair<NettyServer, AtomicInteger> nettyServerIntegerPair = serverMap.get(port);
        if (nettyServerIntegerPair == null) {
            synchronized (serverMap) {
                nettyServerIntegerPair = serverMap.get(port);
                if (nettyServerIntegerPair == null) {
                    NettyServer nettyServer = new NettyServer(port);
                    nettyServerIntegerPair = new Pair<>(nettyServer, new AtomicInteger(0));
                    serverMap.put(port, nettyServerIntegerPair);
                }
            }
        }
        nettyServerIntegerPair.getRight().incrementAndGet();
        return nettyServerIntegerPair.getLeft();
    }

    public static void removeNettyServer(Integer port) {
        Pair<NettyServer, AtomicInteger> nettyServerIntegerPair = serverMap.get(port);
        if (nettyServerIntegerPair != null && nettyServerIntegerPair.getRight().decrementAndGet() == 0) {
            serverMap.remove(port);
            nettyServerIntegerPair.getLeft().close();
        }
    }

    /**
     * 这里存在并发问题，所以使用了双检锁。
     * 当多个接口对应的zk路径同时抖动，可能对应的是同一个服务实例，
     *  一个zk线程进去构建即可，其他的等待构建完成，直接获取即可。
     *
     * 需要计数，如果为0，可以移除，因为可能有多个接口使用同一个ipAndPort
     * @param ipPort
     * @return
     */
    public static NettyClient getNettyClient(String ipPort) {
        // 双检锁，任何对象都可以作为锁
        Pair<NettyClient, AtomicInteger> nettyServerIntegerPair = clientMap.get(ipPort);
        if (nettyServerIntegerPair == null) {
            synchronized (clientMap) {
                nettyServerIntegerPair = clientMap.get(ipPort);
                if (nettyServerIntegerPair == null) {
                    NettyClient nettyServer = new NettyClient(ipPort);
                    nettyServerIntegerPair = new Pair<>(nettyServer, new AtomicInteger(0));
                    clientMap.put(ipPort, nettyServerIntegerPair);
                }
            }
        }
        nettyServerIntegerPair.getRight().incrementAndGet();
        return nettyServerIntegerPair.getLeft();
    }

    public static void removeNettyClient(String ipPort) {
        Pair<NettyClient, AtomicInteger> nettyServerIntegerPair = clientMap.get(ipPort);
        if (nettyServerIntegerPair != null && nettyServerIntegerPair.getRight().decrementAndGet() == 0) {
            clientMap.remove(ipPort);
            nettyServerIntegerPair.getLeft().close();
        }
    }
}
