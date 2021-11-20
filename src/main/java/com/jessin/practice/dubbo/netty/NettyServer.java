package com.jessin.practice.dubbo.netty;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jessin.practice.dubbo.exception.DubboException;
import com.jessin.practice.dubbo.exporter.DubboExporter;
import com.jessin.practice.dubbo.invoker.RpcInvocation;
import com.jessin.practice.dubbo.transport.Request;
import com.jessin.practice.dubbo.transport.Response;
import com.jessin.practice.dubbo.utils.ArgDeserializerUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: jessin
 * @Date: 19-11-27 下午7:40
 */
@Slf4j
public class NettyServer {
    // todo 底层会启动2*cpu个数的NioEventLoop，轮询注册到对应的NioEventLoop运行
    private EventLoopGroup boss = new NioEventLoopGroup();
    private EventLoopGroup worker = new NioEventLoopGroup();
    // 全局复用，是否需要考虑可共享？
    private ServerHandler serverHandler = new ServerHandler();
    private int port;

    public NettyServer(int port) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        // boss线程池用于accept到达的请求，worker线程池对到达的请求进行读写
        // child表示对到达的请求起作用，没有child表示对ServerSocketChannel起作用
        // 服务端用NioServerSocketChannel
        ChannelFuture channelFuture;
        this.port = port;
        try {
            serverBootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // todo option最终设置到jdk sever channel上
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // 对到达的请求进行读写操作，责任链模式，ChannelPipeline
                            ch.pipeline()
                                    .addLast(new BaseDecoder())
                                    .addLast(new BaseEncoder())
                                    .addLast(serverHandler);
                        }
                    });
            // todo bind时，会新建NioServerSocketChannel，并注册到NioEventLoop.selector中
            // todo 底层转换为pipeline.bind()，最终调用serverSocketChannel.bind(socketAddress, 128);
            channelFuture = serverBootstrap.bind(port);
            // 下面会阻塞
            channelFuture.sync();
            log.info("服务器绑定端口：{}成功", port);
            // TODO 关闭时调用，客户端也得关闭
            // channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            throw new RuntimeException("bind port error:" + port, e);
        }
    }

    /**
     * 允许注册到多个客户端SocketChannel中
     */
    @ChannelHandler.Sharable
    class ServerHandler extends ChannelDuplexHandler {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            log.info("客户端:{}和服务端建立连接成功", ctx.channel().remoteAddress());
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // 这里是String类型，已经解码了
            Request request = JSONObject.parseObject((String)msg, Request.class);
            log.info("收到请求消息：{}", msg);
            RpcInvocation rpcInvocation = request.getRpcInvocation();
            Object obj = DubboExporter.getService(rpcInvocation);
            if (obj == null) {
                throw new IllegalStateException("服务端未曝光接口：" + request);
            }
            Response response = new Response();
            response.setId(request.getId());
            try {
                log.info("开始反射调用：{}", msg);
                // todo 这里最好用线程池实现，不然会阻塞NioEventLoop
                Method method = obj.getClass().getMethod(rpcInvocation.getMethodName(), rpcInvocation.getParameterType());
                Object[] originArgs = ArgDeserializerUtils.parseArgs(method, rpcInvocation.getParameterType(), rpcInvocation.getArgs());
                log.info("入参：{}", originArgs);
                Object responseData = method.invoke(obj, originArgs);
                response.setResult(responseData);
                log.info("调用实例：{}，方法：{}，返回结果：{}",
                        obj, method, response);
            } catch (Exception e) {
                log.error("调用dubbo异常：{}", rpcInvocation, e);
                response.setException(true);
                response.setResult(new DubboException("服务端调用接口异常",  e));
            }
            // TODO 通过原来客户端通道发送出去
            // 这里会走编码吗？，必须写成String，或者改下Encoder
            ctx.writeAndFlush(JSON.toJSONString(response));
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            log.info("收到客户端退出的消息");
            ctx.close();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("IO出错了...", cause);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            log.info("发起写请求：{}", msg);
            // TODO 写的一般都有这个？
            super.write(ctx, msg, promise);
        }
    }

    /**
     * dubbo shutdown hook
     */
    public void close() {
        // TODO 这里是否有问题？？
        log.info("关闭端口：{}", port);
        boss.shutdownGracefully();
        worker.shutdownGracefully();
    }
}