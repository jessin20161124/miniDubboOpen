package com.jessin.practice.dubbo.netty;

import com.jessin.practice.dubbo.transport.DefaultFuture;
import com.jessin.practice.dubbo.transport.Request;
import com.jessin.practice.dubbo.transport.Response;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

/**
 * client重复建连，需要共享
 * @Author: jessin
 * @Date: 19-11-25 下午10:25
 */
@ChannelHandler.Sharable
@Slf4j
public class NettyClientHandler extends ChannelDuplexHandler {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 三次握手成功
        ctx.channel().attr(AttributeKey.valueOf(Constants.LAST_READ_KEY)).set(System.currentTimeMillis());
        log.info("客户端和服务端建立连接成功");
        //ctx.writeAndFlush("{\"id\":1,\"rpcInvocation\":{\"interfaceName\":\"com.jessin.practice.dubbo.service.UserService\",\"methodName\":\"getUser\",\"parameterType\":[],\"version\":\"1.0.0\"}}");
    }

    /**
     * 对响应进行处理
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ctx.channel().attr(AttributeKey.valueOf(Constants.LAST_READ_KEY)).set(System.currentTimeMillis());
        //  这里msg直接拿到response
        Response response = (Response)msg;
        if (response.isEvent()) {
            log.info("收到服务端心跳响应：{}", msg);
            return;
        }
        log.info("收到服务端消息：{}", msg);
        DefaultFuture.setResponse(response);
    }

    /**
     * 发送消息前进行拦截，oubound，只有channel.writeAndFlush()才能起作用，active里直接用ctx不起作用
     * @param ctx
     * @param msg
     * @param promise
     * @throws Exception
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        log.info("收到写消息：" + msg);
        // 必须的！保证继续往下走，发送出去，其实就是ctx.write(msg, promise)
        super.write(ctx, msg, promise);
        // promise模式，依赖地狱，以及Future回调模式（guava、java8）
        promise.addListener(future -> {
            // 监听发送回调，看是否发送成功
            if (future.isSuccess()) {
                log.info("发送写消息：{}，成功", msg);
            } else {
                log.info("发送消息失败：{}", msg);
            }
        });
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            // 这里是否需要检测当前连接状态??
            log.info("客户端检测到通道空闲，发送心跳请求");
            // 当前通道空闲了
            Request request = new Request();
            request.setEvent(true);
            ctx.channel().writeAndFlush(request);
        }
        super.userEventTriggered(ctx, evt);
    }
}