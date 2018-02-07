package com.maxwell.nettylearning.netty_private_protocol.handler;

import com.maxwell.nettylearning.netty_private_protocol.message.MessageHeader;
import com.maxwell.nettylearning.netty_private_protocol.message.MessageTypeEnum;
import com.maxwell.nettylearning.netty_private_protocol.message.NettyMessage;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/************************************************************************************
 * 功能描述：
 *
 * 心跳检测：
 *
 * 握手成功之后，由客户端主动发送心跳消息，服务端接收到心跳消息之后，返回心跳应答消息。
 *
 * 由于心跳消息的目的是为了检测链路的可用性，因此不需要消息体。
 *
 * 创建人：岳增存  yueyuemax@gmail.com
 * 创建时间： 2018年02月06日 --  下午9:27 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class HeartBeatRequestChannelHandler extends ChannelHandlerAdapter {

    private volatile ScheduledFuture heartBeatSchedule;

    @Override
    public void channelRead(ChannelHandlerContext context, Object msg) throws Exception {
        NettyMessage message = (NettyMessage) msg;
        MessageHeader header = message.getHeader();

        //握手成功，主动发送心跳消息
        if (header != null && header.getType() == MessageTypeEnum.HAND_SHAKER_RESPONSE.value) {
            //每5秒发送1次心跳消息，握手成功后就一直发送
            heartBeatSchedule = context.executor().scheduleAtFixedRate(() -> {
                NettyMessage beatRequest = buildHeartBeatRequest();
                context.writeAndFlush(beatRequest);
                System.out.println("客户端给服务端发送心跳请求消息：" + beatRequest.toString());
            }, 0, 5, TimeUnit.SECONDS);
            //收到服务器发送的心跳响应消息，无需做任何处理
        } else if (header != null && header.getType() == MessageTypeEnum.HEART_RESPONSE.value) {
            System.out.println("客户端收到了服务端的心跳响应消息：" + message.toString());
        } else {
            //其他消息，直接透传给后续的ChannelHandler
            context.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if(heartBeatSchedule != null){
            heartBeatSchedule.cancel(true);
            heartBeatSchedule = null;
        }
        cause.printStackTrace();
        ctx.fireExceptionCaught(cause);

    }

    private NettyMessage buildHeartBeatRequest() {
        NettyMessage message = new NettyMessage();
        MessageHeader header = new MessageHeader();
        header.setType(MessageTypeEnum.HEART_REQUEST.value);
        message.setHeader(header);
        return message;
    }
}
