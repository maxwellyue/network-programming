package com.maxwell.nettylearning.time_server_netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yuezc@seentao.com
 * 创建时间： 2018年02月03日 --  下午3:26 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class TimeClient {

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 8080;
        new TimeClient().connect(host, port);
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
                            ch.pipeline().addLast(new TimeClientHandler());
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

    private class TimeClientHandler extends ChannelHandlerAdapter {

        /**
         * 客户端与服务器成功建立连接之后的回调
         * @param ctx
         * @throws Exception
         */
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            //向服务器发送消息
            byte[] bytes = "query time".getBytes();
            ByteBuf request = Unpooled.buffer(bytes.length);
            request.writeBytes(bytes);
            ctx.writeAndFlush(request);
            System.out.println("客户端向服务器发送消息：query time");
        }

        /**
         * 当通道有消息可读的回调，即接收到服务器发来的消息后的回调
         * @param ctx
         * @param msg
         * @throws Exception
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            //读取服务器发来的消息
            ByteBuf buf = (ByteBuf) msg;
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            String response = new String(bytes, "UTF-8");
            System.out.println("客户端接收到服务器发来的消息：" + response);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }


}
