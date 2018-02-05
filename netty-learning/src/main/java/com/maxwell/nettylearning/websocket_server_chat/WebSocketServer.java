package com.maxwell.nettylearning.websocket_server_chat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.UUID;

/************************************************************************************
 * 功能描述：
 *
 * 简单的聊天室的实现
 *
 * 省略了一些判断错误或异常的代码，只保留与主题相关的代码
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
                            ch.pipeline().addLast(new WebSocketServerHandler());
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


    private static class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

        //注意：ChannelGroup要设置为静态变量，因为不是每个连接私有的
        private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

        private WebSocketServerHandshaker handshaker;

        //当前连接的用户
        private User user;

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
                        user = new User();
                        user.setName(UUID.randomUUID().toString().substring(0, 8));
                        channelGroup.writeAndFlush(new TextWebSocketFrame("[系统消息]欢迎[" + user.getName() + "]进入聊天室"));
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
                channelGroup.remove(context.channel());
                handshaker.close(context.channel(), (CloseWebSocketFrame) frame.retain());
                channelGroup.writeAndFlush(new TextWebSocketFrame("[系统消息][用户" + user.getName() + "]退出了聊天室"));
                return;
            }
            //如果是Ping消息
            if (frame instanceof PingWebSocketFrame) {
                context.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
                return;
            }

            broadcast(context, ((TextWebSocketFrame)frame).text());
        }

        public void broadcast(ChannelHandlerContext context, String msg) {
            String response = "[用户" +user.getName() + "]说：" + msg;
            channelGroup.writeAndFlush(new TextWebSocketFrame(response));
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
