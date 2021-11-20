package com.jessin.practice.dubbo.netty;

import com.alibaba.fastjson.JSON;
import com.jessin.practice.dubbo.transport.DefaultFuture;
import com.jessin.practice.dubbo.transport.Response;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: jessin
 * @Date: 19-11-25 下午10:25
 */
@Slf4j
public class NettyClientHandler extends ChannelDuplexHandler {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
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
        log.info("收到服务端消息：" + msg);
        // result字段为JSONObject
        Response response = JSON.parseObject((String)msg, Response.class);
        // todo 返回list/map，带复杂key/value的是否有问题
        DefaultFuture.setResponse(response);
    }

    /**
     * TODO 发送消息前进行拦截，oubound，只有channel.writeAndFlush()才能起作用，active里直接用ctx不起作用
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
        // TODO promise模式，依赖地狱，以及Future回调模式（guava、java8）
        promise.addListener(future -> {
            // 监听发送回调，看是否发送成功
            if (future.isSuccess()) {
                log.info("发送写消息：{}，成功", msg);
            } else {
                log.info("发送消息失败：{}", msg);
            }
        });
    }
}