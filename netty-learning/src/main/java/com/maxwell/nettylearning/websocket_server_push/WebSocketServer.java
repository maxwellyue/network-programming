package com.maxwell.nettylearning.websocket_server_push;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/************************************************************************************
 * 功能描述：
 *
 * netty websocket + continuous PERIODIC data push
 *
 * 参考链接：https://stackoverflow.com/questions/10901399/netty-websocket-continuous-periodic-data-push
 *
 * 1、WebSocket client connects to your Netty server, handshakes and establishes a websocket connection.
 * 2、The server registers the client's channel somewhere where it can be retrieved when there's data to send. (I use a ChannelGroup in a singleton)
 * 3、The scheduled job fires, gets some data from somewhere, then gets a reference to the client's channel and writes the data to it.
 * 4、The client channel's pipeline should have a few encoders in it that marshal the scheduled job supplied data into websocket frames.
 *
 *
 * 创建人：岳增存  yueyuemax@gmail.com
 * 创建时间： 2018年02月05日 --  下午3:59 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class WebSocketServer {

    public static void main(String[] args) {
        int port = 8080;
        new WebSocketServer().run(port);
    }

    public void run(int port) {
        //配置服务端的NIO线程组：NioEventLoopGroup是一个线程组，专门处理网络事件
        //这两个线程组，一个负责接收客户端的连接，另一个负责SocketChannel的网络读写
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    //设置channel类型
                    .channel(NioServerSocketChannel.class)
                    //I/O事件的处理类
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //将请求和应答消息编码或解码为Http消息
                            ch.pipeline().addLast("http-codec", new HttpServerCodec());
                            //将多个Http消息转换为单一的FullHttpRequest或者FullHttpResponse
                            ch.pipeline().addLast("aggregator", new HttpObjectAggregator(65536));
                            //支持异步发送大的码流（例如大的文件传输），但不会占用过多内存
                            ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
                            //具体的业务逻辑处理
                            ch.pipeline().addLast(new WebSocketServerHandler(ChannelGroupEnum.channelGroup()));
                        }
                    });

            //绑定端口，等待成功
            Channel channel = bootstrap.bind(port).sync().channel();
            System.out.println("WebSocket服务器已启动");
            //等待服务器监听端口关闭
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //关闭线程池资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }


    private class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

        private ChannelGroup channelGroup;

        private WebSocketServerHandshaker handshaker;

        public WebSocketServerHandler(ChannelGroup channelGroup){
            this.channelGroup = channelGroup;
        }

        @Override
        protected void messageReceived(ChannelHandlerContext context, Object msg) throws Exception {
            //WebSocket的握手请求消息由HTTP协议来承载，所以它是一个Http请求
            if (msg instanceof FullHttpRequest) {
                handlerHttpRequest(context, (FullHttpRequest) msg);
            } else if (msg instanceof WebSocketFrame) {
                handleWebSocketFrame(context, (WebSocketFrame) msg);
            }
        }


        /**
         * 处理Http请求
         *
         * @param context
         * @param request
         */
        private void handlerHttpRequest(ChannelHandlerContext context, FullHttpRequest request) {
            //处理WebSocket的握手请求：WebSocket的握手请求消息由HTTP协议来承载
            WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory("ws://localhost:8080/websocket", null, false);
            handshaker = wsFactory.newHandshaker(request);
            if (handshaker == null) {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(context.channel());
            } else {
                //handshake()不仅会发送WebSocket握手响应给客户端，还会同时将WebSocket相关的编解码器添加到channel的pipeline中，
                //这一点，可以看一下handshake()方法的源码进行验证
                ChannelFuture future = handshaker.handshake(context.channel(), request);
                try {
                    future.sync();
                    if (future.isSuccess()) {
                        channelGroup.add(context.channel());
                        System.out.println("WebSocket握手成功");
                        //握手成功后，开始向客户端定时发送消息
                        scheduledTask();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * 处理WebSocket请求
         *
         * @param context
         * @param frame
         */
        private void handleWebSocketFrame(ChannelHandlerContext context, WebSocketFrame frame) {

            //如果是关闭链路的指令
            if (frame instanceof CloseWebSocketFrame) {
                ChannelGroupEnum.channelGroup().remove(context.channel());
                handshaker.close(context.channel(), (CloseWebSocketFrame) frame.retain());
                return;
            }
            //如果是Ping消息
            if (frame instanceof PingWebSocketFrame) {
                context.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
                return;
            }
        }

        public void scheduledTask() {
            Executors.newSingleThreadScheduledExecutor()
                    .scheduleAtFixedRate(new Runnable() {
                        @Override
                        public void run() {
                            String data = "[现在时间是：" + new Date().toString() + "]";
                            System.out.println("向客户端发送：" + data);
                            ChannelGroupEnum.channelGroup().writeAndFlush(new TextWebSocketFrame(data));
                        }
                    }, 0, 4, TimeUnit.SECONDS);

        }

        @Override
        public void channelReadComplete(ChannelHandlerContext context) throws Exception {
            context.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();
        }
    }


}
