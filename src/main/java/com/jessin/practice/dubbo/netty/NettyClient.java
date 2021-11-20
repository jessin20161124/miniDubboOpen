package com.jessin.practice.dubbo.netty;

import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
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

    private NettyClientHandler clientHandler = new NettyClientHandler();

    public NettyClient(String ipAndPort) {
        this.ipAndPort = ipAndPort;
        connect();
    }

    public void connect() {
        log.info("建立netty连接：{}", ipAndPort);
        Bootstrap bootstrap = new Bootstrap();
        try {
            bootstrap.group(worker).channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        // TODO 注意pipeline的顺序
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    // clientHandler可以提为全局变量
                                    .addLast(new BaseEncoder())
                                    .addLast(new BaseDecoder())
                                    .addLast(clientHandler);
                        }
                    });
            // 客户端是connect
            String[] values = ipAndPort.split(":");
            // TODO 考虑超时重连，心跳断开重连，底层转换为pipeline.connect()
            ChannelFuture channelFuture = bootstrap.connect(values[0], Integer.parseInt(values[1])).sync();
            if (channelFuture.isSuccess()) {
                log.info("与服务端建立连接成功：{}", ipAndPort);
            } else {
                log.error("与服务端建立连接失败:{}", ipAndPort);
            }
            // 建立连接时保存下来，可能有需要连接多个客户端
            this.socketChannel = channelFuture.channel();
        } catch (Exception e) {
            log.error("与服务端建立连接失败:{}", ipAndPort, e);
            throw new RuntimeException("与服务端建立连接失败: " + ipAndPort, e);
        }
    }

    /**
     * 对外发送数据接口
     * @param msg
     */
    public void send(Object msg) {
        // TODO 必须用writeAndFlush才会真正发出去，同时必须序列化为字符串，才能被编码继续往下走
        String jsonStr = JSON.toJSONString(msg);
        socketChannel.writeAndFlush(jsonStr);
    }

    public void close() {
        log.info("关闭访问服务的连接：{}", ipAndPort);
        socketChannel.close();
        if (socketChannel != null && socketChannel.isActive()) {
            try {
                socketChannel.closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        worker.shutdownGracefully();
    }
}
