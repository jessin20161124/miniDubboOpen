package com.jessin.practice.dubbo.netty;

/**
 * @Author: jessin
 * @Date: 2022/1/1 11:49 上午
 */
import com.jessin.practice.dubbo.exporter.DubboExporter;
import com.jessin.practice.dubbo.invoker.RpcInvocation;
import com.jessin.practice.dubbo.transport.Request;
import com.jessin.practice.dubbo.transport.Response;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;

/**
 * 允许注册到多个客户端SocketChannel中
 */
@ChannelHandler.Sharable
@Slf4j
public class NettyServerHandler extends ChannelDuplexHandler {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 这里，估计需要维护所有的客户端
        log.info("客户端:{}和服务端建立连接成功", ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 已经解码了
        Request request = (Request)msg;
        log.info("收到请求消息：{}", msg);
        RpcInvocation rpcInvocation = request.getRpcInvocation();
        Object obj = DubboExporter.getService(rpcInvocation);
        Response response = new Response();
        response.setId(request.getId());
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
        // 写回调
        super.write(ctx, msg, promise);
    }
}