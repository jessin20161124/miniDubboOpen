package com.jessin.practice.dubbo.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * todo protocol buf
 * @Author: jessin
 * @Date: 19-11-25 下午10:20
 */
public class BaseEncoder extends MessageToByteEncoder<String> {
    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Encode a message into a {@link ByteBuf}. This method will be called for each written message that can be handled
     * by this encoder.
     *
     * @param ctx the {@link ChannelHandlerContext} which this {@link MessageToByteEncoder} belongs to
     * @param msg the message to encode
     * @param out the {@link ByteBuf} into which the encoded message will be written
     * @throws Exception is thrown if an error occurs
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, String msg, ByteBuf out) throws Exception {
        logger.info("对消息：{}进行编码", msg);
        byte[] wordBytes = msg.getBytes("utf-8");
        out.writeInt(wordBytes.length);
        out.writeBytes(wordBytes);
    }
}
