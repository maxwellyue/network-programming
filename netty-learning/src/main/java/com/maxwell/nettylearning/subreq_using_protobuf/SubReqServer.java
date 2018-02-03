package com.maxwell.nettylearning.subreq_using_protobuf;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yueyuemax@gmail.com
 * 创建时间： 2018年02月03日 --  下午9:30 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class SubReqServer {

    public static void main(String[] args) {
        int port = 8080;
        new SubReqServer().bind(port);
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
                    //设置SO_BACKLOG为100
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    //I/O事件的处理类
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                            //需要对请求消息进行解码
                            ch.pipeline().addLast(new ProtobufDecoder(SubscribeReqProto.SubscribeReq.getDefaultInstance()));
                            ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                            ch.pipeline().addLast(new ProtobufEncoder());
                            ch.pipeline().addLast(new SubReqServerHandler());
                        }
                    });

            //绑定端口，等待成功
            ChannelFuture future = bootstrap.bind(port).sync();
            System.out.println("服务器已启动");

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


    @ChannelHandler.Sharable
    private class SubReqServerHandler extends ChannelHandlerAdapter {

        private int count;

        @Override
        public void channelRead(ChannelHandlerContext context, Object msg) throws Exception {
            SubscribeReqProto.SubscribeReq request = (SubscribeReqProto.SubscribeReq) msg;
            //todo 中文字符是8进制的字符，不能正常显示
            System.out.println("服务器收到客服端发来的消息：" + request + "，当前次数：" + ++count);
            if ("maxwell".equalsIgnoreCase(request.getUsername())) {
                context.writeAndFlush(resp(request.getSubReqID()));
            }
        }

        private SubscribeRespProto.SubscribeResp resp(int subReqID) {
            SubscribeRespProto.SubscribeResp.Builder builder = SubscribeRespProto.SubscribeResp.newBuilder();
            builder.setSubReqID(subReqID);
            builder.setRespCode(0);
            builder.setDesc("订购《Netty 权威指南》成功，预计3天后送达");
            return builder.build();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
