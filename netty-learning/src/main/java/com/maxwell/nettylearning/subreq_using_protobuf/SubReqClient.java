package com.maxwell.nettylearning.subreq_using_protobuf;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yueyuemax@gmail.com
 * 创建时间： 2018年02月03日 --  下午10:42 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class SubReqClient {

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 8080;
        new SubReqClient().connect(host, port);
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
                    .handler(new LoggingHandler(LogLevel.INFO))
                    //添加handler
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //处理半包消息
                            ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                            //需要对响应消息进行解码
                            ch.pipeline().addLast(new ProtobufDecoder(SubscribeRespProto.SubscribeResp.getDefaultInstance()));
                            ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                            ch.pipeline().addLast(new ProtobufEncoder());
                            ch.pipeline().addLast(new SubReqClientHandler());
                        }
                    });

            //发起异步连接，并调用同步方法等待连接成功
            ChannelFuture future = bootstrap.connect(host, port).sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    private class SubReqClientHandler extends ChannelHandlerAdapter {

        private int count;

        /**
         * 客户端与服务器成功建立连接之后的回调
         *
         * @param ctx
         * @throws Exception
         */
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            //向服务器发送消息
            for (int i = 0; i < 10; i++) {
                SubscribeReqProto.SubscribeReq req = subReq(i);
                ctx.write(req);
                //发送10次
                System.out.println("客户端向服务端发送消息：" + req.toString());
            }
            ctx.flush();
        }

        private SubscribeReqProto.SubscribeReq subReq(int i) {
            SubscribeReqProto.SubscribeReq.Builder builder = SubscribeReqProto.SubscribeReq.newBuilder();
            builder.setSubReqID(i)
                    .setUsername("maxwell")
                    .setProductName("《Netty 权威指南》")
                    .addAddress("北京")
                    .addAddress("上海")
                    .addAddress("广州");
            return builder.build();
        }

        /**
         * 当通道有消息可读的回调，即接收到服务器发来的消息后的回调
         *
         * @param ctx
         * @param msg
         * @throws Exception
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            //读取服务器发来的消息
            SubscribeRespProto.SubscribeResp resp = (SubscribeRespProto.SubscribeResp) msg;
            //todo 中文字符是8进制的字符，不能正常显示，如 address: "\344\270\212\346\265\267"
            System.out.println("客户端接收到服务器发来的消息：" + resp + "，当前次数：" + ++count);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
