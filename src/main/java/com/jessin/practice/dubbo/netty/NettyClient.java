package com.jessin.practice.dubbo.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;


/**
 * @Author: jessin
 * @Date: 19-11-25 下午10:17
 */
@Slf4j
public class NettyClient {

    private String ipAndPort;
    /**
     * worker可以共用
     */
    private EventLoopGroup worker = new NioEventLoopGroup();

    private Channel socketChannel;

    private ConnectState state = ConnectState.INIT;

    // 多个client共享
    private static NettyClientHandler clientHandler = new NettyClientHandler();

    /**
     * 每个client一个独立的定时线程....
     */
    private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    public NettyClient(String ipAndPort) {
        this.ipAndPort = ipAndPort;
        connect();
        // 至少1秒运行一次
        long duration = Math.max(Constants.HEARTBEAT_TIMEOUT_MILLIS / 3, 1000);
        // todo cancel
        // 用delay而不是fixed，保证如果连接卡住了，还会休眠duration再重试
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            // 可能重入，和开始线程一起进入连接，或者在重连时，有业务线程获取状态，这里需要考虑并发。
            // 这里考虑的是初始化没有连接成功时，可以无限定时重连...，考虑重连次数...
            if(!isConnected()) {
                log.info("初始连接未成功，重连：{}", ipAndPort);
                connect();
            } else {
                // 客户端上一次收到服务端回复的时间戳
                Object lastReadTimestamp = socketChannel.attr(AttributeKey.valueOf(Constants.LAST_READ_KEY)).get();
                if (lastReadTimestamp != null
                        && System.currentTimeMillis() - (Long)lastReadTimestamp >= Constants.HEARTBEAT_TIMEOUT_MILLIS) {
                    log.info("心跳超时没有响应，重连：{}", ipAndPort);
                    reconnect();
                }
            }
        }, 0, duration, TimeUnit.MILLISECONDS);
    }

    public void reconnect() {
        disconnect();
        connect();
    }

    public synchronized void connect() {
        if (isConnected()) {
            return;
        }
        state = ConnectState.CONNECTING;
        log.info("建立netty连接：{}", ipAndPort);
        Bootstrap bootstrap = new Bootstrap();
        try {
            bootstrap.group(worker).channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        // 注意pipeline的顺序
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new BaseEncoder())
                                    .addLast(new BaseDecoder())
                                    // 空闲触发器，超过心跳时间没有发送或收到数据时，发送心跳
                                    .addLast(new IdleStateHandler(0, 0, Constants.HEARTBEAT_INTERVAL_MILLIS, TimeUnit.MILLISECONDS))
                                    .addLast(clientHandler);
                        }
                    });
            // 客户端是connect
            String[] values = ipAndPort.split(":");
            //  底层转换为pipeline.connect()
            ChannelFuture channelFuture = bootstrap.connect(values[0], Integer.parseInt(values[1])).sync();
            if (channelFuture.isSuccess()) {
                state = ConnectState.CONNECTED;
                log.info("与服务端建立连接成功：{}", ipAndPort);
            } else {
                log.error("与服务端建立连接失败:{}", ipAndPort);
                state = ConnectState.CONNECT_FAIL;
            }
            // 建立连接时保存下来，可能有需要连接多个客户端
            this.socketChannel = channelFuture.channel();
        } catch (Exception e) {
            log.error("与服务端建立连接失败:{}", ipAndPort, e);
            state = ConnectState.CONNECT_FAIL;
           // throw new RuntimeException("与服务端建立连接失败: " + ipAndPort, e);
        }
    }

    public synchronized boolean isConnected() {
        return state == ConnectState.CONNECTED && socketChannel != null && socketChannel.isActive();
    }

    /**
     * 对外发送数据接口
     * @param msg
     */
    public void send(Object msg) {
        if (!isConnected()) {
            throw new RuntimeException("channel is closed!");
        }
        // 必须用writeAndFlush才会真正发出去
        socketChannel.writeAndFlush(msg);
    }

    /**
     * 断连时不销毁资源，重连时还要用
     */
    public synchronized void disconnect() {
        if (socketChannel != null && socketChannel.isActive()) {
            try {
                socketChannel.close().sync();
                socketChannel = null;
            } catch (InterruptedException e) {
                log.error("断连受到干扰：{}", ipAndPort, e);
            }
        }
    }

    /**
     * 关闭包括断开连接和销毁其他资源
     * todo 这种关闭比较粗暴，是否等待一定时间再关闭？优雅关闭
     */
    public void close() {
        log.info("关闭访问服务的连接，销毁整个nettyClient：{}", ipAndPort);
        disconnect();
        worker.shutdownGracefully();
        // 立即关闭
        scheduledExecutorService.shutdownNow();
        state = ConnectState.CLOSED;
    }
}
