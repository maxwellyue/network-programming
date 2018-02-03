package com.maxwell.nettylearning.echo_using_fixed_length;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yueyuemax@gmail.com
 * 创建时间： 2018年02月03日 --  下午8:28 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class EchoClient {

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 8080;
        new EchoClient().connect(host, port);
    }

    public void connect(String host, int port) {
        //创建处理IO读写的线程组
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            //创建辅助启动类
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    //指定channel类型
                    .channel(NioSocketChannel.class)
                    //设置TCP_NODELAY为true
                    .option(ChannelOption.TCP_NODELAY, true)
                    //添加handler
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new StringDecoder());
                            ch.pipeline().addLast(new EchoClientHandler());
                        }
                    });

            //发起异步连接，并调用同步方法等待连接成功
            ChannelFuture future = bootstrap.connect(host, port).sync();
            //
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    private class EchoClientHandler extends ChannelHandlerAdapter {

        /**
         * 客户端与服务器成功建立连接之后的回调
         * @param ctx
         * @throws Exception
         */
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            String msg = "Hello,Netty";
            //向服务器发送消息
            for(int i = 0; i < 100; i++){
                ctx.writeAndFlush(Unpooled.copiedBuffer(msg.getBytes()));
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
