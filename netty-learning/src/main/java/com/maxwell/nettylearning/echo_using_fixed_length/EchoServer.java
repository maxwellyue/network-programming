package com.maxwell.nettylearning.echo_using_fixed_length;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yuezc@seentao.com
 * 创建时间： 2018年02月03日 --  下午8:19 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class EchoServer {

    public static void main(String[] args){
        int port = 8080;
        new EchoServer().bind(port);
    }


    public void bind(int port) {
        //配置服务端的NIO线程组：NioEventLoopGroup是一个线程组，专门处理网络事件
        //这两个线程组，一个负责接收客户端的连接，另一个负责SocketChannel的网络读写
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(bossGroup, workerGroup)
                    //设置channel类型
                    .channel(NioServerSocketChannel.class)
                    //设置SO_BACKLOG为1024
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    //I/O事件的处理类
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //在此，添加定长解码器，则会按定长进行分割客户端发来的消息
                            //假如，客户端发送100次消息，每次长度为11，则服务端会收到50个消息
                            ch.pipeline().addLast(new FixedLengthFrameDecoder(22));
                            ch.pipeline().addLast(new StringDecoder());
                            ch.pipeline().addLast(new EchoServerHandler());
                        }
                    });

            //绑定端口，等待成功
            ChannelFuture future = bootstrap.bind(port).sync();
            System.out.println("Echo服务器已启动");

            //等待服务器监听端口关闭
            future.channel().closeFuture().sync();
        }catch (InterruptedException e){
            e.printStackTrace();
        }finally {
            //关闭线程池资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }


    private class EchoServerHandler extends ChannelHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext context, Object msg) throws Exception {
            String request = (String)msg;
            System.out.println("服务器收到客服端发来的消息：" + request);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();
        }
    }
}
