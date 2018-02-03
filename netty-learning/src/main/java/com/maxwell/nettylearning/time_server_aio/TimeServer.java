package com.maxwell.nettylearning.time_server_aio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yuezc@seentao.com
 * 创建时间： 2018年02月03日 --  下午1:30 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class TimeServer {

    public static void main(String[] args) {
        int port = 8080;
        new Thread(new TimeServerHandler(port)).start();
    }

    static class TimeServerHandler implements Runnable {

        private int port;
        private CountDownLatch latch;
        private AsynchronousServerSocketChannel serverSocketChannel;

        public TimeServerHandler(int port) {
            this.port = port;
            try {
                //出初始化
                serverSocketChannel = AsynchronousServerSocketChannel.open();
                serverSocketChannel.bind(new InetSocketAddress(port));
                System.out.println("时间服务器已启动");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            latch = new CountDownLatch(1);

            //异步接收客户端的连接
            serverSocketChannel.accept(this, new CompletionHandler<AsynchronousSocketChannel, TimeServerHandler>() {

                /**
                 * 连接成功后的回调
                 *
                 * @param result
                 * @param handler
                 */
                @Override
                public void completed(AsynchronousSocketChannel result, TimeServerHandler handler) {
                    //连接成功后，继续循环监听下一个客户端连接
                    handler.serverSocketChannel.accept(handler, this);
                    //异步将通道中的数据读取到缓冲区中
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    result.read(buffer, buffer, new ReadCompletionHandler(result));
                }

                @Override
                public void failed(Throwable exc, TimeServerHandler handler) {
                    exc.printStackTrace();
                    handler.latch.countDown();
                }
            });

            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }


    /**
     * 读取完成的处理器
     */
    static class ReadCompletionHandler implements CompletionHandler<Integer, ByteBuffer>{

        private AsynchronousSocketChannel channel;

        public ReadCompletionHandler(AsynchronousSocketChannel channel){
            this.channel = channel;
        }

        /**
         * 读取消息之后的操作
         * @param result
         * @param attachment
         */
        @Override
        public void completed(Integer result, ByteBuffer attachment) {
            attachment.flip();
            byte[] body = new byte[attachment.remaining()];
            attachment.get(body);
            try {
                String request = new String(body, "UTF-8");
                System.out.println("服务器收到客户端的消息：" + request);
                String currentTime = "query time".equalsIgnoreCase(request) ? new Date().toString() : "bad order";
                //向客户端发送响应消息，即当前时间
                doWrite(currentTime);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {
            try {
                this.channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * 向客户端发送消息
         * @param response
         */
        private void doWrite(String response){
            byte[] bytes = response.getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            writeBuffer.put(bytes);
            writeBuffer.flip();
            channel.write(writeBuffer, writeBuffer, new CompletionHandler<Integer, ByteBuffer>() {

                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    if(attachment.hasRemaining()){
                        channel.write(attachment, attachment, this);
                    }
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    try {
                        channel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
