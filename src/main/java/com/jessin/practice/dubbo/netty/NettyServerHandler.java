package com.jessin.practice.dubbo.netty;

/**
 * @Author: jessin
 * @Date: 2022/1/1 11:49 上午
 */

import com.jessin.practice.dubbo.exporter.DubboExporter;
import com.jessin.practice.dubbo.invoker.RpcInvocation;
import com.jessin.practice.dubbo.transport.Request;
import com.jessin.practice.dubbo.transport.Response;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleStateEvent;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * 允许注册到多个客户端SocketChannel中
 */
@ChannelHandler.Sharable
@Slf4j
public class NettyServerHandler extends ChannelDuplexHandler {

    private Map<Channel, Channel> clientChanel = new ConcurrentHashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端:{}和服务端建立连接成功", ctx.channel().remoteAddress());
        // 这里维护所有的客户端，长时间没有数据到达时进行关闭
        clientChanel.put(ctx.channel(), ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("收到客户端退出的消息");
        clientChanel.remove(ctx.channel());
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 已经解码了
        Request request = (Request)msg;
        Response response = new Response();
        response.setId(request.getId());
        if (request.isEvent()) {
            log.info("收到心跳请求消息：{}，进行响应", msg);
            // 通过原来客户端通道发送出去，这里会走编码
            response.setEvent(true);
            ctx.writeAndFlush(response);
            return;
        }
        log.info("收到请求消息：{}", msg);
        RpcInvocation rpcInvocation = request.getRpcInvocation();
        Object obj = DubboExporter.getService(rpcInvocation);
        try {
            if (obj == null) {
                response.setException(true);
                response.setResult(new RuntimeException("no provider"));
            } else {
                log.info("开始反射调用：{}", msg);
                // todo 这里最好用线程池实现，不然会阻塞NioEventLoop
                // 这里所有参数都是正确的，有泛化信息也是对的，由序列化层解决
                Method method = obj.getClass().getMethod(rpcInvocation.getMethodName(), rpcInvocation.getParameterType());
                log.info("入参：{}", rpcInvocation.getArgs());
                // filter...
                Object responseData = method.invoke(obj, rpcInvocation.getArgs());
                response.setResult(responseData);
                log.info("调用实例：{}，方法：{}，返回结果：{}",
                        obj, method, response);
            }
        } catch (Exception e) {
            log.error("调用dubbo异常：{}", rpcInvocation, e);
            response.setException(true);
            response.setResult(e);
        }
        // 通过原来客户端通道发送出去，这里会走编码
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("IO出错了...", cause);
    }

    /**
     * 写回调
     * @param ctx
     * @param msg
     * @param promise
     * @throws Exception
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        log.info("发起写请求：{}", msg);
        super.write(ctx, msg, promise);
    }

    /**
     * 服务端检测到某个client channel空闲时，直接关闭。客户端会定时发起心跳，如果超过一定时间没有数据到达，
     * 说明客户端挂了或者网络有问题，需要关闭客户端
     *
     *
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            log.info("超时时间到达，关闭客户端：{}", ctx.channel());
            clientChanel.remove(ctx.channel());
            ctx.channel().close();
        }
        super.userEventTriggered(ctx, evt);
    }
}