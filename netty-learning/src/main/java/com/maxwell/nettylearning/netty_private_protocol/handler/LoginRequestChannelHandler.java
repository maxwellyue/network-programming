package com.maxwell.nettylearning.netty_private_protocol.handler;

import com.maxwell.nettylearning.netty_private_protocol.message.MessageHeader;
import com.maxwell.nettylearning.netty_private_protocol.message.MessageTypeEnum;
import com.maxwell.nettylearning.netty_private_protocol.message.NettyMessage;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

/************************************************************************************
 * 功能描述：
 *
 * 握手认证：在TCP连接激活时发起握手请求
 *
 * 创建人：岳增存  yueyuemax@gmail.com
 * 创建时间： 2018年02月06日 --  上午11:27 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class LoginRequestChannelHandler extends ChannelHandlerAdapter {

    /**
     * 通道连接时：
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //发送握手请求消息
        NettyMessage loginRequest = buildLoginRequest();
        ctx.writeAndFlush(loginRequest);
        System.out.println("客户端发送握手请求消息：" + loginRequest.toString());
    }

    /**
     * 收到握手请求消息的响应消息时：
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NettyMessage message = (NettyMessage) msg;
        //如果是握手应答消息，需要判断是否认证成功
        if (message.getHeader() != null && message.getHeader().getType() == MessageTypeEnum.HAND_SHAKER_RESPONSE.value) {
            System.out.println("客户端收到服务端发来的握手响应消息：" + message.toString());
            byte loginResult = (byte) message.getBody();
            if (loginResult != (byte) 0) {
                //握手失败，关闭连接
                ctx.close();
            } else {
                //握手成功
                System.out.println("客户端登录认证成功");
                //透传给后面的ChannelHandler进行处理
                ctx.fireChannelRead(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.fireExceptionCaught(cause);
    }

    private NettyMessage buildLoginRequest() {
        NettyMessage message = new NettyMessage();
        MessageHeader header = new MessageHeader();
        header.setType(MessageTypeEnum.HAND_SHAKER_REQUEST.value);
        message.setHeader(header);
        return message;
    }
}
