package com.maxwell.nettylearning.netty_private_protocol.codec;

import com.maxwell.nettylearning.netty_private_protocol.message.NettyMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.Map;

/************************************************************************************
 * 功能描述：
 *
 * Netty消息编码类
 *
 * 创建人：岳增存  yueyuemax@gmail.com
 * 创建时间： 2018年02月06日 --  上午10:30
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class NettyMessageEncoder extends MessageToByteEncoder<NettyMessage> {

    MarshallingEncoder marshallingEncoder;

    public NettyMessageEncoder() throws Exception {
        this.marshallingEncoder = new MarshallingEncoder();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, NettyMessage msg, ByteBuf sendBuf) throws Exception {
        if (msg == null || msg.getHeader() == null) {
            throw new Exception("The encode message is null");
        }
        //写入数据，按照NettyMessage的头部信息
        sendBuf.writeInt(msg.getHeader().getCrcCode());
        sendBuf.writeInt(msg.getHeader().getLength());
        sendBuf.writeLong(msg.getHeader().getSessionId());
        sendBuf.writeByte(msg.getHeader().getType());
        sendBuf.writeByte(msg.getHeader().getPriority());
        Map<String, Object> attachment= msg.getHeader().getAttachment();
        sendBuf.writeInt(attachment != null ? attachment.size() : 0);
        String key = null;
        byte[] keyBytes = null;
        Object value = null;
        //使用JBoss Marshalling 处理消息体
        for (Map.Entry<String, Object> param : msg.getHeader().getAttachment().entrySet()) {
            key = param.getKey();
            keyBytes = key.getBytes("UTF-8");
            sendBuf.writeInt(keyBytes.length);
            sendBuf.writeBytes(keyBytes);
            value = param.getValue();
            marshallingEncoder.encode(value, sendBuf);
        }
        //处理NettyMessage body部分
        if (msg.getBody() != null) {
            marshallingEncoder.encode(msg.getBody(), sendBuf);
        } else {
            sendBuf.writeInt(0);
        }
        sendBuf.setInt(4, sendBuf.readableBytes() - 8);
    }

}
