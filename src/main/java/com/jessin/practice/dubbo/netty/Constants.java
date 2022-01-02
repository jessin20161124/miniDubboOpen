package com.jessin.practice.dubbo.netty;

/**
 * @Author: jessin
 * @Date: 2021/12/30 9:27 下午
 */
public interface Constants {
    byte REQUEST = 0;
    byte RESPONSE = 1;

    /**
     *     心跳探测机制：定时空闲检测连接状态，及早关闭无效连接释放资源或重连，保持连接活跃。这里是客户端主动发起模式，需要避免太频繁，占用带宽
     *     客户端定时（HEARTBEAT_INTERVAL_MILLIS）发送心跳请求，达到超时时间（HEARTBEAT_TIMEOUT_MILLIS）没有收到心跳响应，则重连。对应两个定时任务
     *     服务端收到请求，需要返回心跳响应，定时（HEARTBEAT_TIMEOUT_MILLIS）检测通道是否有收到请求，如果没有，直接关闭客户端。
     *
     *     例如服务端或者客户端宕机
     *     客户端网络有问题，
     *     如果服务端负载过高（例如内部业务逻辑有死循环，导致线程池满了），可以将请求转移到正常的服务端节点。
     *
     *
     *     服务端超时时间必须是客户端心跳时间的2倍及以上。例如，
     *     client: 0, 3s,  6s, 9s, 12s, 15s, 18s, 21s不断发送心跳，9s/18s检查，没有收到响应，则重连
     *     server:             9s检查，      18s检查   如果发现超过9s没有发生读事件，说明客户端有问题，或者客户端网络有问题，关闭客户端连接，否则继续保留
      */
    int HEARTBEAT_INTERVAL_MILLIS = 3000;

    int HEARTBEAT_TIMEOUT_MILLIS = 3 * HEARTBEAT_INTERVAL_MILLIS;

    String LAST_READ_KEY = "lastReadTimestamp";

    Serializer SERIALIZER = new FastjsonSerializer();
}
