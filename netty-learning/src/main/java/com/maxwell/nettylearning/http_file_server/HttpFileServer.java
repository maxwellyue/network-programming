package com.maxwell.nettylearning.http_file_server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yueyuemax@gmail.com
 * 创建时间： 2018年02月04日 --  下午12:32 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class HttpFileServer {

    public static void main(String[] args) {
        //服务器IP
        String host = "127.0.0.1";
        //服务器端口
        int port = 8080;
        //文件服务器的根目录
        String rootUrl = "/Users/yue/Documents/workspace/idea/network-programming";
        //启动文件服务器
        new HttpFileServer().run(host, port, rootUrl);
    }

    private void run(String host, int port, String url) {
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup worker = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //对请求解码
                            ch.pipeline().addLast("http-request-decoder", new HttpRequestDecoder());
                            //将多个Http消息转换为单一的FullHttpRequest或者FullHttpResponse
                            ch.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
                            //对响应编码
                            ch.pipeline().addLast("http-response-encoder", new HttpResponseEncoder());
                            //支持异步发送大的码流（例如大的文件传输），但不会占用过多内存
                            ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
                            //具体的逻辑处理
                            ch.pipeline().addLast(new HttpFileServerHandler(url));
                        }
                    });
            ChannelFuture future = bootstrap.bind(host, port);
            System.out.println("文件服务器已启动，访问地址是：http://" + host + ":" + port + url);
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    public class HttpFileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        private final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");
        private final Pattern ALLOWED_FILE_NAME = Pattern.compile("[A-Za-z0-9][-_A-Za-z0-9\\.]*");

        private final String rootUrl;

        public HttpFileServerHandler(String rootUrl) {
            this.rootUrl = rootUrl;
        }

        @Override
        public void messageReceived(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
            //请求解码失败
            if (!request.decoderResult().isSuccess()) {
                sendError(context, BAD_REQUEST);
                return;
            }
            //非GET请求
            if (request.method() != GET) {
                sendError(context, METHOD_NOT_ALLOWED);
                return;
            }

            //获取请求的Uri
            final String uri = request.uri();
            //对uri消毒处理，并返回拼装好的文件路径
            final String path = sanitizeUri(uri);
            //如果路径为空
            if (path == null) {
                sendError(context, FORBIDDEN);
                return;
            }

            //根据路径创建文件
            File file = new File(path);
            //如果是隐藏文件或文件不存在
            if (file.isHidden() || !file.exists()) {
                sendError(context, NOT_FOUND);
                return;
            }

            //响应请求
            if (file.isDirectory()) {
                if (uri.endsWith("/")) {
                    sendFileList(context, file);
                } else {
                    sendRedirect(context, uri + '/');
                }
            } else if (file.isFile()) {
                sendFileContent(context, file, request);
            } else {
                sendError(context, FORBIDDEN);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            if (ctx.channel().isActive()) {
                sendError(ctx, INTERNAL_SERVER_ERROR);
            }
        }

        /**
         * 校验Uri的合法性
         *
         * @param uri
         * @return
         */
        private String sanitizeUri(String uri) {
            //对URI解码
            try {
                uri = URLDecoder.decode(uri, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                try {
                    uri = URLDecoder.decode(uri, "ISO-8859-1");
                } catch (UnsupportedEncodingException e1) {
                    e.printStackTrace();
                }
            }

            //访问的URI必须是在设定的根目录下
            if (!uri.startsWith(rootUrl)) {
                return null;
            }

            //将路径中的/替换为系统的文件分隔符
            uri = uri.replace('/', File.separatorChar);

            //判断文件路径是否合法
            if (uri.contains(File.separator + '.')
                    || uri.contains('.' + File.separator)
                    || uri.startsWith(".")
                    || uri.endsWith(".")
                    || INSECURE_URI.matcher(uri).matches()) {
                return null;
            }
            return uri;
        }


        /**
         * 向客户端发送文件内容
         *
         * @param context
         * @param file
         * @param request
         * @throws IOException
         */
        private void sendFileContent(ChannelHandlerContext context, File file, FullHttpRequest request) throws IOException {
            RandomAccessFile randomAccessFile = null;
            try {
                //以只读的方式打开文件
                randomAccessFile = new RandomAccessFile(file, "r");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                sendError(context, NOT_FOUND);
                return;
            }

            //获取文件长度
            long fileLength = randomAccessFile.length();

            //构造响应头
            HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
            //设置content-type
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, new MimetypesFileTypeMap().getContentType(file.getPath()));
            //设置content-length
            HttpHeaderUtil.setContentLength(response, fileLength);
            //设置连接的keep-alive属性
            if (HttpHeaderUtil.isKeepAlive(request)) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderNames.KEEP_ALIVE);
            }
            //将响应头信息返回给客户端
            context.write(response);
            //将响应体信息返回给客户端
            context.write(new ChunkedFile(randomAccessFile, 0, fileLength, 8192), context.newProgressivePromise())
                    //传输进度监听
                    .addListener(new ChannelProgressiveFutureListener() {
                        @Override
                        public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                            // total unknown
                            if (total < 0) {
                                System.err.println("Transfer progress: " + progress);
                            } else {
                                System.err.println("Transfer progress: " + progress + " / " + total);
                            }
                        }

                        @Override
                        public void operationComplete(ChannelProgressiveFuture future) throws Exception {
                            System.out.println("Transfer complete.");
                        }
                    });
            //如果使用Chunked编码，最后需要发送一个编码结束的空消息体，标识所有的消息体都已经发送完毕
            ChannelFuture lastContentFuture = context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            if (!HttpHeaderUtil.isKeepAlive(request)) {
                lastContentFuture.addListener(ChannelFutureListener.CLOSE);
            }
        }

        /**
         * 向客户端发送文件列表
         *
         * @param context
         * @param dir
         */
        private void sendFileList(ChannelHandlerContext context, File dir) {
            //构造响应消息
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
            //设置响应头：content-type
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
            //拼接响应体的内容
            String content = spellFileListHtmlContent(dir);

            //将拼接的信息写在响应体中
            ByteBuf buffer = Unpooled.copiedBuffer(content, CharsetUtil.UTF_8);
            response.content().writeBytes(buffer);
            //释放缓冲区
            buffer.release();
            //将响应消息返回给客户端
            context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        /**
         * 拼接当用户访问合法目录时的页面内容
         * @param dir 用户访问的合法目录
         * @return
         */
        private String spellFileListHtmlContent(File dir) {
            //拼接响应体的内容
            StringBuilder buf = new StringBuilder();
            String dirPath = dir.getPath();

            buf.append("<!DOCTYPE html>\r\n");
            buf.append("<html><head><title>");
            buf.append(dirPath);
            buf.append(" 目录：");
            buf.append("</title></head><body>\r\n");
            buf.append("<h3>");
            buf.append(dirPath).append(" 目录：");
            buf.append("</h3>\r\n");
            buf.append("<ul>");
            buf.append("<li><a href=\"../\">返回上一级目录</a></li>\r\n");
            for (File f : dir.listFiles()) {
                //跳过隐藏文件和不可读文件
                if (f.isHidden() || !f.canRead()) {
                    continue;
                }
                String name = f.getName();
                if (!ALLOWED_FILE_NAME.matcher(name).matches()) {
                    continue;
                }
                buf.append("<li>链接：<a href=\"");
                buf.append(name);
                buf.append("\">");
                buf.append(name);
                buf.append("</a></li>\r\n");
            }
            buf.append("</ul></body></html>\r\n");
            return buf.toString();
        }


        /**
         * 向客户端发送重定向
         *
         * @param ctx
         * @param newUri
         */
        private void sendRedirect(ChannelHandlerContext ctx, String newUri) {
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FOUND);
            response.headers().set(HttpHeaderNames.LOCATION, newUri);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
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
