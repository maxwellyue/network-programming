package com.maxwell.nettylearning.websocket_server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;

import java.util.Date;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/************************************************************************************
 * 功能描述：
 *
 *
 * WebSocket服务器接收到请求消息后，先对消息类型进行判断，
 * 如果不是WebSocket握手请求消息，则返回400给客户端；
 * 如果是，则对握手消息进行处理，构造握手响应消息并返回，双方的Socket连接正式建立。
 *
 * 连接建立成功后，到被关闭之前，双方都可以主动向对方发送消息。
 * 与Http不同的是，它的网络利用率更高，消息的发送和接收是全双工的。
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


    private class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

        private WebSocketServerHandshaker handshaker;

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
            //如果不是WebSocket握手请求，返回400
            if (!request.decoderResult().isSuccess() || !"websocket".equals(request.headers().get("Upgrade"))) {
                sendHttpResponse(context, request, new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
                return;
            }

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
                    if(future.isSuccess()){
                        System.out.println("WebSocket握手成功");
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
                handshaker.close(context.channel(), (CloseWebSocketFrame) frame.retain());
                return;
            }
            //如果是Ping消息
            if (frame instanceof PingWebSocketFrame) {
                context.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
                return;
            }

            //本例子只支持文本消息，不支持二进制消息
            if (!(frame instanceof TextWebSocketFrame)) {
                throw new UnsupportedOperationException(String.format("不支持 %s frame类型", frame.getClass().getName()));
            }

            //返回应答消息
            String request = ((TextWebSocketFrame) frame).text();
            System.out.println("收到客户端的消息：" + request);
            String response = request + "，欢迎使用Netty WebSocket服务，现在时刻：" + new Date().toString();
            context.channel().write(new TextWebSocketFrame(response));
        }


        /**
         * 给客户端发送Http响应
         *
         * @param context
         * @param request
         * @param response
         */
        private void sendHttpResponse(ChannelHandlerContext context, FullHttpRequest request, FullHttpResponse response) {
            if (response.status() != HttpResponseStatus.OK) {
                ByteBuf buf = Unpooled.copiedBuffer(response.status().toString(), CharsetUtil.UTF_8);
                //将buf中内容写到response.content()中
                response.content().writeBytes(buf);
                buf.release();
                HttpHeaderUtil.setContentLength(response, response.content().readableBytes());
            }

            ChannelFuture future = context.channel().writeAndFlush(response);

            if (!HttpHeaderUtil.isKeepAlive(request) || response.status() != HttpResponseStatus.OK) {
                future.addListener(ChannelFutureListener.CLOSE);
            }
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
