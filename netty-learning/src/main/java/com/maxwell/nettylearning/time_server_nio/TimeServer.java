package com.maxwell.nettylearning.time_server_nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yuezc@seentao.com
 * 创建时间： 2018年02月02日 --  下午8:16 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class TimeServer {


    public static void main(String[] args){
        int port = 8080;

        new Thread(new TimeServerHandler(port)).start();
    }

    static class TimeServerHandler implements Runnable{

        private Selector selector;
        private ServerSocketChannel serverSocketChannel;
        private volatile boolean stop;

        /**
         * 构造器，完成初始化
         * @param port
         */
        public TimeServerHandler(int port){
            try{
                //①创建Selector
                selector = Selector.open();
                //②创建ServerSocketChannel
                serverSocketChannel = ServerSocketChannel.open();
                //③配置ServerSocketChannel：非阻塞，并绑定端口，设置backlog
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.socket().bind(new InetSocketAddress(port), 1024);
                //④将ServerSocketChannel注册到Selector上，并监听OP_ACCEPT操作位
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                System.out.println("时间服务器已经启动");
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        public void stop(){
            this.stop = true;
        }

        @Override
        public void run() {
            //循环遍历Selector
            while (!stop){
                try {
                    //阻塞获取当前处于就绪状态的Channel，最多等待1000ms
                    selector.select(1000);
                    //返回SelectionKey集合
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    //对selectionKeys进行遍历，依次进行网络的异步读写操作
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    SelectionKey selectionKey = null;
                    while (iterator.hasNext()){
                        selectionKey = iterator.next();
                        iterator.remove();
                        try{
                            //对selectionKey进行处理
                            handlerInput(selectionKey);
                        }catch (IOException e){
                            if(selectionKey != null){
                                selectionKey.cancel();
                                if(selectionKey.channel() != null){
                                    selectionKey.channel().close();
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        public void handlerInput(SelectionKey key) throws IOException{
            if(key.isValid()){
                //如果当前key对应的Channel已经可以接受新的socket连接
                if(key.isAcceptable()){
                    //获取key对应的channel
                    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                    //接受客户端的连接请求，创建SocketChannel实例（创建后，即已经完成了TCP的三次握手）
                    SocketChannel channel = serverChannel.accept();
                    //配置当前channel为非阻塞，并注册到selector上
                    channel.configureBlocking(false);
                    channel.register(selector, SelectionKey.OP_READ);
                }

                //如果当前key对应的Channel可以读取数据，则获取客户端的连接请求，读取数据
                if(key.isReadable()){
                    SocketChannel channel = (SocketChannel) key.channel();
                    ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                    //由于SocketChannel设置为异步非阻塞模式，因此channel.read()方法是非阻塞的
                    int readBytes = channel.read(readBuffer);

                    if(readBytes > 0){
                        //flip()：将缓冲区当前的limit设置为position，position设置为0，以便后续读取
                        readBuffer.flip();
                        byte[] bytes = new byte[readBuffer.remaining()];
                        readBuffer.get(bytes);

                        String body = new String(bytes, "UTF-8");
                        System.out.println("时间服务器收到来自客户端的消息：" + body);
                        String currentTime = "query time".equalsIgnoreCase(body) ? new Date().toString() : "bad order";
                        //将响应消息发送给客户端
                        doWrite(channel, currentTime);
                    }else if(readBytes < 0){
                        key.cancel();
                        channel.close();
                    }else {
                        //do nothing
                    }
                }
            }
        }

        private void doWrite(SocketChannel channel, String response) throws IOException {
            //将响应消息转换为字节数组
            byte[] bytes = response.getBytes();
            //创建缓冲区，并将响应消息的字节数组复制到缓冲区
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            writeBuffer.put(bytes);
            //对缓冲区执行flip()：将buffer的当前位置更改为buffer缓冲区的第一个位置
            writeBuffer.flip();
            //将缓冲区的数据从channel发送出去
            channel.write(writeBuffer);
            System.out.println("服务器发送给客户端消息：" + response);
        }
    }


}
