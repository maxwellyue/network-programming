package com.maxwell.nettylearning.http_server_json;

import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;

import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yueyuemax@gmail.com
 * 创建时间： 2018年02月04日 --  下午2:58 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class HttpJsonServer {

    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int port = 8080;
        new HttpJsonServer().run(host, port);
    }

    private void run(String host, int port) {
        // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            //HTTP 服务的解码器
                            ch.pipeline().addLast(new HttpServerCodec());
                            //HTTP 消息的合并处理
                            ch.pipeline().addLast(new HttpObjectAggregator(2048));
                            //自己的业务逻辑处理
                            ch.pipeline().addLast(new JsonServerHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind(host, port);
            System.out.println("服务器已启动，访问地址是：http://" + host + ":" + port);
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public class JsonServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        @Override
        protected void messageReceived(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

            String uri = request.uri();

            Object respContent = null;

            switch (uri) {
                case "/":
                    respContent = "Welcome !!!";
                    break;
                case "/mate":
                    //把客户端的JSON格式的请求数据转换为Java对象
                    String reqStr = request.content().toString(CharsetUtil.UTF_8);
                    Person person = JSON.parseObject(reqStr, Person.class);
                    respContent = JSON.toJSONString("this guy has no mate");
                    if ("maxwell".equalsIgnoreCase(person.getName())) {
                        Person mate = new Person();
                        mate.setName("little maxwell");
                        mate.setAddress("china beijing");
                        mate.setAge(18);
                        respContent = mate;
                    }
                    break;
                default:
                    sendError(ctx, NOT_FOUND);
                    return;
            }
            //向客户端发送结果
            ResponseJson(ctx, request, JSON.toJSONString(respContent));
        }

        /**
         * 向客户端发送响应消息
         *
         * @param context
         * @param req
         * @param respContent
         */
        private void ResponseJson(ChannelHandlerContext context, FullHttpRequest req, String respContent) {
            boolean keepAlive = HttpHeaderUtil.isKeepAlive(req);
            byte[] jsonByteByte = respContent.getBytes();
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(jsonByteByte));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/json");
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            if (!keepAlive) {
                context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                response.headers().set(HttpHeaderNames.CONNECTION, KEEP_ALIVE);
                context.writeAndFlush(response);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }


        /**
         * 向客户端发送错误信息
         *
         * @param ctx
         * @param status
         */
        private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
                    status, Unpooled.copiedBuffer("Failure: " + status.toString()
                    + "\r\n", CharsetUtil.UTF_8));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
