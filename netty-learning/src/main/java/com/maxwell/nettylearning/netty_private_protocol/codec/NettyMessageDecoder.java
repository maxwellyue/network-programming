package com.maxwell.nettylearning.netty_private_protocol.codec;

import com.maxwell.nettylearning.netty_private_protocol.message.MessageHeader;
import com.maxwell.nettylearning.netty_private_protocol.message.NettyMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/************************************************************************************
 * 功能描述：
 *
 * Netty消息解码类，继承LengthFieldBasedFrameDecoder解码器，支持自动的TCP粘包和半包处理
 *
 * 创建人：岳增存  yueyuemax@gmail.com
 * 创建时间： 2018年02月06日 --  上午10:30
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class NettyMessageDecoder extends LengthFieldBasedFrameDecoder {

    MarshallingDecoder marshallingDecoder;

    /**
     *
     * LengthFieldBasedFrameDecoder 可以自动的处理TCP粘包和拆包问题，
     *
     * 只需要给出标识消息长度的字段偏移量和消息长度自身所占的字节数
     *
     * Netty就能自动实现对半包的处理
     *
     * @param maxFrameLength 消息最大长度
     * @param lengthFieldOffset 消息长度字段的偏移量
     * @param lengthFieldLength 消息长度字段本身的长度
     * @throws IOException
     */
    public NettyMessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) throws IOException {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
        marshallingDecoder = new MarshallingDecoder();
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }

        NettyMessage message = new NettyMessage();

        MessageHeader header = new MessageHeader();
        header.setCrcCode(frame.readInt());
        header.setLength(frame.readInt());
        header.setSessionId(frame.readLong());
        header.setType(frame.readByte());
        header.setPriority(frame.readByte());

        int size = frame.readInt();
        if (size > 0) {
            Map<String, Object> attch = new HashMap<String, Object>(size);
            int keySize = 0;
            byte[] keyArray = null;
            String key = null;
            for (int i = 0; i < size; ++i) {
                keySize = frame.readInt();
                keyArray = new byte[keySize];
                frame.readBytes(keyArray);
                key = new String(keyArray, "UTF-8");
                attch.put(key, marshallingDecoder.decode(frame));
            }
            header.setAttachment(attch);
        }
        if (frame.readableBytes() > 4) {
            message.setBody(marshallingDecoder.decode(frame));
        }
        message.setHeader(header);
        return message;
    }


}
