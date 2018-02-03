package com.maxwell.nettylearning.time_server_netty_solving_sticky_package_problem;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import java.util.Date;

/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yuezc@seentao.com
 * 创建时间： 2018年02月03日 --  下午3:25 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class TimeServer {

    public static void main(String[] args) {
        int port = 8080;
        new TimeServer().bind(port);
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
                    .option(ChannelOption.SO_BACKLOG, 1014)
                    //I/O事件的处理类
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
                            ch.pipeline().addLast(new StringDecoder());
                            ch.pipeline().addLast(new TimeServerHandler());
                        }
                    });

            //绑定端口，等待成功
            ChannelFuture future = bootstrap.bind(port).sync();
            System.out.println("时间服务器已启动");

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


    private class TimeServerHandler extends ChannelHandlerAdapter {

        private int count;

        @Override
        public void channelRead(ChannelHandlerContext context, Object msg) throws Exception {
            //每接收到一次消息，就打印一次
            String request = (String)msg;
            System.out.println("服务器收到客服端发来的消息：" + request + "，计数器的当前值为：" + ++count);

            String currentTime = "query time".equalsIgnoreCase(request) ? new Date().toString() : "bad order";
            //发给客户端的消息要以换行符为分隔符，这样客户端就可以解决TCP粘包问题
            currentTime += System.getProperty("line.separator");
            ByteBuf responseBuffer = Unpooled.copiedBuffer(currentTime.getBytes());
            //异步向客户端发送消息，write只是将消息放到消息发送队列，并不是真正发送给SocketChannel
            //还要继续调用flush()
            context.write(responseBuffer);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext context) throws Exception {
            //将消息发送队列中的消息写入到SocketChannel，发送给对方
            ChannelHandlerContext flush = context.flush();
            System.out.println("服务器将消息发送给客户端");
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();
        }
    }
}

