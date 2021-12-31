package com.jessin.practice.dubbo.netty;

import com.jessin.practice.dubbo.transport.Request;
import com.jessin.practice.dubbo.transport.Response;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * todo 协议规范化
 * @Author: jessin
 * @Date: 19-11-25 下午10:20
 */
public class BaseEncoder extends MessageToByteEncoder {
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
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        logger.info("对消息：{}进行编码", msg);
        byte flag;
        if (msg instanceof Request) {
            flag = Constants.REQUEST;
        } else if (msg instanceof Response) {
            flag = Constants.RESPONSE;
        } else {
            throw new UnsupportedOperationException("flag unknown:" + msg);
        }
        byte[] wordBytes = Constants.SERIALIZER.serialize(msg);
        out.writeInt(wordBytes.length + 1);
        out.writeByte(flag);
        out.writeBytes(wordBytes);
    }
}
