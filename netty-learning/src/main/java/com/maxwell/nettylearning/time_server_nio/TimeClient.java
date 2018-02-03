package com.maxwell.nettylearning.time_server_nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yuezc@seentao.com
 * 创建时间： 2018年02月02日 --  下午8:17 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class TimeClient {

    public static void main(String[] args){
        String host = "127.0.0.1";
        int port = 8080;
        new Thread(new TimeClientHandler(host, port)).start();
        while (true){

        }
    }

    static class TimeClientHandler implements Runnable{

        private String host;
        private int port;
        private Selector selector;
        private SocketChannel socketChannel;
        private volatile boolean stop;

        public TimeClientHandler(String host, int port){
            this.host = host;
            this.port = port;
            try {
                selector = Selector.open();
                socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(false);
            }catch (IOException e){
                e.printStackTrace();
                System.exit(1);
            }
        }

        @Override
        public void run() {
            //连接到服务器，并注册到selector中的读操作上
            try {
                //异步连接到服务器
                if(socketChannel.connect(new InetSocketAddress(host, port))){
                    //注册到Selector的读操作位
                    socketChannel.register(selector, SelectionKey.OP_READ);
                    //发送消息给服务器
                    doWrite(socketChannel);
                }else {
                    //连接失败，则注册到Selector的连接操作位
                    //这里的连接失败，是指没有收到服务器的TCP握手应答消息，
                    //此时将其注册到SelectionKey.OP_CONNECT上，当服务器返回TCP的syn-ack消息后，
                    //selector就可以轮询到这个socketChannel处于连接就绪状态
                    //这样就实现了异步连接，而不必像BIO一样，阻塞直到连接成功
                    socketChannel.register(selector, SelectionKey.OP_CONNECT);
                }
            }catch (IOException e){
                e.printStackTrace();
                System.exit(1);
            }

            //向服务器发送消息，并接受消息
            while (!stop){
                try {
                    selector.select(1000);
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    SelectionKey key = null;
                    while (iterator.hasNext()){
                        key = iterator.next();
                        iterator.remove();
                        try {
                            handlerInput(key);
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }

            //关闭多路复用器
            //多路复用器关闭后，所有注册在上面的Channel和Pipe等资源都会自动关闭，不必显式去关闭它们。
            if(selector != null){
                try {
                    selector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handlerInput(SelectionKey key) throws IOException {
            if(key.isValid()){
                SocketChannel channel = (SocketChannel) key.channel();
                //如果连接成功（收到了服务器的ACK应答消息），则向服务器发送消息，并注册到读操作上
                if(key.isConnectable()){
                    if(channel.finishConnect()){
                        channel.register(selector, SelectionKey.OP_READ);
                        //向服务器发送消息
                        doWrite(channel);
                    }else {
                        //连接失败，退出进程
                        System.exit(1);
                    }
                }
                //如果接收到了服务器发送的消息，则读取，同时关闭本连接
                if(key.isReadable()){
                    ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                    //将通道中的数据读到缓冲区中
                    int readBytes = channel.read(readBuffer);
                    if(readBytes > 0){
                        readBuffer.flip();
                        byte[] bytes = new byte[readBuffer.remaining()];
                        //将缓冲区的数据读到字节数组中
                        readBuffer.get(bytes);
                        String body = new String(bytes, "UTF-8");
                        System.out.println("现在时间是：" + body);
                        this.stop = true;
                    }else if(readBytes < 0){
                        key.cancel();
                        channel.close();
                    }
                }
            }

        }

        /**
         * 向服务器发送消息
         *
         * @param channel
         * @throws IOException
         */
        private void doWrite(SocketChannel channel) throws IOException {
            byte[] request = "query time".getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(request.length);
            writeBuffer.put(request);
            writeBuffer.flip();
            //由于发送是异步的，可能会存在"读半包"问题，这里没有处理
            channel.write(writeBuffer);
            //判断消息是否全部发送
            if(!writeBuffer.hasRemaining()){
                System.out.println("客户端成功向时间服务器发送查询指令");
            }
        }
    }

}
