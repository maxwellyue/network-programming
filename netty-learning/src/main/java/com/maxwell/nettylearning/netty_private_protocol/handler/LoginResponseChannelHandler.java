package com.maxwell.nettylearning.netty_private_protocol.handler;

import com.maxwell.nettylearning.netty_private_protocol.message.MessageHeader;
import com.maxwell.nettylearning.netty_private_protocol.message.MessageTypeEnum;
import com.maxwell.nettylearning.netty_private_protocol.message.NettyMessage;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/************************************************************************************
 * 功能描述：
 *
 * 登录响应：即对握手请求的响应处理
 *
 * 创建人：岳增存  yueyuemax@gmail.com
 * 创建时间： 2018年02月06日 --  下午8:37 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class LoginResponseChannelHandler extends ChannelHandlerAdapter {

    private Map<String, Boolean> nodeChecked = new ConcurrentHashMap<>();

    private List<String> whiteList = Arrays.asList("127.0.0.1", "192.168.1.104");

    @Override
    public void channelRead(ChannelHandlerContext context, Object msg) throws Exception {
        NettyMessage message = (NettyMessage) msg;
        //如果是握手请求消息
        if (message.getHeader() != null && message.getHeader().getType() == MessageTypeEnum.HAND_SHAKER_REQUEST.value) {
            //获取客户端地址
            String nodeIndex = context.channel().remoteAddress().toString();
            //构造对握手请求的响应消息
            NettyMessage loginResponse = null;
            //如果已经登录过了，则返回失败（防止重复登录，重复登录会造成句柄泄漏）
            if (nodeChecked.containsKey(nodeIndex)) {
                loginResponse = buildResponse((byte) -1);
            } else {
                //进行登录校验：白名单中是否含有，有则通过，没有则拒绝
                InetSocketAddress address = (InetSocketAddress) context.channel().remoteAddress();
                String ip = address.getAddress().getHostAddress();
                if (whiteList.contains(ip)) {
                    loginResponse = buildResponse((byte) 0);
                    nodeChecked.put(nodeIndex, true);
                } else {
                    //如果白名单不含有该IP，则失败
                    loginResponse = buildResponse((byte) -1);
                }
            }
            //返回对握手请求的应答消息
            context.writeAndFlush(loginResponse);
            System.out.println("服务端返回握手响应消息：" + loginResponse.toString());
        } else {
            //非握手消息，直接透传给下一个ChannelHandler处理
            context.fireChannelRead(msg);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //链路关闭时，将该链路对应的客户端从nodeChecked删除，以保证后续该客户端可以重连成功
        nodeChecked.remove(ctx.channel().remoteAddress().toString());
        ctx.close();
        cause.printStackTrace();
        ctx.fireExceptionCaught(cause);
    }

    /**
     * 构建对握手请求的响应消息
     *
     * @param result
     * @return
     */
    private NettyMessage buildResponse(byte result) {
        NettyMessage message = new NettyMessage();
        MessageHeader header = new MessageHeader();
        header.setType(MessageTypeEnum.HAND_SHAKER_RESPONSE.value);
        message.setHeader(header);
        message.setBody(result);
        return message;
    }
}
