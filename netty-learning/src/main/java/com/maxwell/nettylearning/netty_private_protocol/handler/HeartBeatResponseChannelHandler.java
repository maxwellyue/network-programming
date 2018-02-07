package com.maxwell.nettylearning.netty_private_protocol.handler;

import com.maxwell.nettylearning.netty_private_protocol.message.MessageHeader;
import com.maxwell.nettylearning.netty_private_protocol.message.MessageTypeEnum;
import com.maxwell.nettylearning.netty_private_protocol.message.NettyMessage;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

/************************************************************************************
 * 功能描述：
 *
 * 心跳响应：即对心跳请求消息的处理
 *
 * 创建人：岳增存  yueyuemax@gmail.com
 * 创建时间： 2018年02月06日 --  下午9:51 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class HeartBeatResponseChannelHandler extends ChannelHandlerAdapter{

    @Override
    public void channelRead(ChannelHandlerContext context, Object msg) throws Exception {
        NettyMessage message = (NettyMessage)msg;

        //对心跳请求消息进行响应
        if(message.getHeader() != null && message.getHeader().getType() == MessageTypeEnum.HEART_REQUEST.value){
            System.out.println("服务端收到客户端发来的心跳消息：" + message.toString());
            //构造心跳响应消息
            NettyMessage heartBeatResponse = buildHeartBeatResponse();
            //给客户端返回心跳响应消息
            context.writeAndFlush(heartBeatResponse);
            System.out.println("服务端发送心跳消息给客户端：" + heartBeatResponse.toString());
        }else {
            //非心跳请求消息，直接透传给下一个ChannelHandler处理
            context.fireChannelRead(msg);
        }
    }

    private NettyMessage buildHeartBeatResponse(){
        NettyMessage message = new NettyMessage();
        MessageHeader header = new MessageHeader();
        header.setType(MessageTypeEnum.HEART_RESPONSE.value);
        message.setHeader(header);
        return message;
    }
}
