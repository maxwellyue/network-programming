package com.maxwell.nettylearning.netty_private_protocol;

import com.maxwell.nettylearning.netty_private_protocol.codec.NettyMessageDecoder;
import com.maxwell.nettylearning.netty_private_protocol.codec.NettyMessageEncoder;
import com.maxwell.nettylearning.netty_private_protocol.handler.HeartBeatResponseChannelHandler;
import com.maxwell.nettylearning.netty_private_protocol.handler.LoginResponseChannelHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;

/************************************************************************************
 * 功能描述：
 *
 * Netty私有协议的服务端
 *
 * 主要工作就是对握手请求消息的接入认证，即响应握手消息
 *
 * 不关系断连重连事件（由客户端进行重连）
 *
 *
 * 创建人：岳增存  yueyuemax@gmail.com
 * 创建时间： 2018年02月06日 --  下午10:25 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class NettyServer {

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 8080;
        new NettyServer().bind(host, port);
    }

    public void bind(String host, int port) {
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
                            ChannelPipeline pipeline = ch.pipeline();
                            //用于NettyMessage的解码
                            pipeline.addLast(new NettyMessageDecoder(1024 * 1024, 4, 4));
                            //用于NettyMessage的编码
                            pipeline.addLast(new NettyMessageEncoder());
                            //超时重连机制：当一定时间内没有读取到对方的任何消息，会主动关闭链路
                            pipeline.addLast(new ReadTimeoutHandler(50));
                            //响应握手请求
                            pipeline.addLast(new LoginResponseChannelHandler());
                            //响应心跳请求
                            pipeline.addLast(new HeartBeatResponseChannelHandler());
                        }
                    });
            //绑定端口，等待成功
            ChannelFuture future = bootstrap.bind(host, port).sync();
            System.out.println("Netty私有协议服务器已启动");
            //等待服务器监听端口关闭
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //关闭线程池资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
