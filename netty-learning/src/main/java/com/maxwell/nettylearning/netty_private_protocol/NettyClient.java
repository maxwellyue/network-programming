package com.maxwell.nettylearning.netty_private_protocol;

import com.maxwell.nettylearning.netty_private_protocol.codec.NettyMessageDecoder;
import com.maxwell.nettylearning.netty_private_protocol.codec.NettyMessageEncoder;
import com.maxwell.nettylearning.netty_private_protocol.handler.HeartBeatRequestChannelHandler;
import com.maxwell.nettylearning.netty_private_protocol.handler.LoginRequestChannelHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/************************************************************************************
 * 功能描述：
 *
 * Netty私有协议的客户端
 *
 *
 *
 *
 * 创建人：岳增存  yueyuemax@gmail.com
 * 创建时间： 2018年02月06日 --  下午9:59 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class NettyClient {

    private ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    public static void main(String[] args) {
        String remoteHost = "127.0.0.1";
        int remotePort = 8080;
        String localHost = "127.0.0.1";
        int localPort = 12088;
        new NettyClient().connect(remoteHost, remotePort, localHost, localPort);
    }

    public void connect(String remoteHost, int remotePort, String localHost, int localPort) {
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .channel(NioSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .handler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            //用于NettyMessage的解码：消息最大长度为1024*1024，消息长度自身偏移量为4，消息字段本身字节数为4字节
                            pipeline.addLast(new NettyMessageDecoder(1024 * 1024, 4, 4));
                            //用于NettyMessage的编码
                            pipeline.addLast(new NettyMessageEncoder());
                            //超时重连机制：当一定时间内没有读取到对方的任何消息，会主动关闭链路
                            pipeline.addLast(new ReadTimeoutHandler(50));
                            //用来发送握手请求
                            pipeline.addLast(new LoginRequestChannelHandler());
                            //用来发送心跳请求
                            pipeline.addLast(new HeartBeatRequestChannelHandler());
                        }
                    });
            //发起连接
            ChannelFuture channelFuture = bootstrap.connect(new InetSocketAddress(remoteHost, remotePort), new InetSocketAddress(localHost, localPort)).sync();
            //监听网络断链事件：除非链路关闭，否则会一直阻塞在这里，即对closeFuture进行sync
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //注意，如果一切运行正常，则程序不会执行到这里，除非出现异常
            //出现异常后，链路关闭，因此客户端需要重新发起连接
            //todo 为什么断连是这个逻辑？closeFuture()？为什么要用定时器？
            scheduledExecutor.execute(() -> {
                try {
                    //等待所有资源释放完成
                    TimeUnit.SECONDS.sleep(5);
                    //发起重连
                    System.out.println("客户端发起重连");
                    connect(remoteHost, remotePort, localHost, localPort);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
