package com.jessin.practice.dubbo.netty;


/**
 * 发送的字节数 + 字节信息
 * 按照这个格式进行装包和拆包，主要是会产生粘包的现象
 * 也就是发送方按照abc, def, 发送
 * 接收方收到的可能是a,bc,de,f，面向的是字节流，需要拆包解出命令
 *
 * @author jessin
 * @create 19-11-25 下午10:20
 **/
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
        int readableBytes = in.readableBytes();
        if (totalBytes == -1 && readableBytes >= 4) {
            totalBytes = in.readInt();
        }
        int secondReadableBytes = in.readableBytes();
        if (totalBytes > 0 && secondReadableBytes >= totalBytes) {
            byte[] realData = new byte[totalBytes];
            in.readBytes(realData);
            out.add(new String(realData, "utf-8"));
            totalBytes = -1;
        }
        logger.info("读取字节个数：{}，剩余字节个数：{}", readableBytes, secondReadableBytes);
    }
}
