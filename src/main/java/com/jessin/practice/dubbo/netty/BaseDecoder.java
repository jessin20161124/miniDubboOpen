package com.jessin.practice.dubbo.netty;


/**
 * 发送的字节数 + 字节信息
 * 按照这个格式进行装包和拆包，主要是会产生粘包的现象
 * 也就是发送方按照abc, def, 发送
 * 接收方收到的可能是a,bc,de,f，面向的是字节流，需要拆包解出命令
 *
 * 编解码层，实现字节流到对象，body一般需要序列化，请求头不用，序列化需要实现跨语言
 * @author jessin
 * @create 19-11-25 下午10:20
 **/

import com.jessin.practice.dubbo.transport.Request;
import com.jessin.practice.dubbo.transport.Response;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseDecoder extends ByteToMessageDecoder {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private int totalBytes = -1;

    /**
     * Decode the from one {@link ByteBuf} to an other. This method will be called till either the input
     * {@link ByteBuf} has nothing to read when return from this method or till nothing was read from the input
     * {@link ByteBuf}.
     *
     * @param ctx the {@link ChannelHandlerContext} which this {@link ByteToMessageDecoder} belongs to
     * @param in  the {@link ByteBuf} from which to read data
     * @param out the {@link List} to which decoded messages should be added
     * @throws Exception is thrown if an error occurs
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // todo 是否做循环？？out是一个list，需要限制网络请求大小，例如dubbo默认最大是8MB
        int readableBytes = in.readableBytes();
        if (totalBytes == -1 && readableBytes >= 4) {
            totalBytes = in.readInt();
        }
        int secondReadableBytes = in.readableBytes();
        if (totalBytes > 1 && secondReadableBytes >= totalBytes) {
            // todo 实际应该支持多个对象序列化，正常情况下是一个一个字段序列化，定制协议，而不是一整个进行序列化
            byte flag = in.readByte();
            byte[] realData = new byte[totalBytes - 1];
            in.readBytes(realData);
            Object req;
            if (flag == Constants.REQUEST) {
                req = Constants.SERIALIZER.deserialize(realData, Request.class);
            } else if (flag == Constants.RESPONSE) {
                req = Constants.SERIALIZER.deserialize(realData, Response.class);
            } else {
                throw new UnsupportedOperationException("flag unknown:" + flag);
            }
            out.add(req);
            totalBytes = -1;
        }
        logger.info("读取字节个数：{}，剩余字节个数：{}", readableBytes, secondReadableBytes);
    }
}
