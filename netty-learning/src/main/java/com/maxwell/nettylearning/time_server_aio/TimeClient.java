package com.maxwell.nettylearning.time_server_aio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yuezc@seentao.com
 * 创建时间： 2018年02月03日 --  下午2:39 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class TimeClient {

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 8080;
        new Thread(new TimeClientHandler(host, port)).start();

        while (true) {

        }
    }

    static class TimeClientHandler implements Runnable, CompletionHandler<Void, TimeClientHandler> {

        private String host;
        private int port;
        private AsynchronousSocketChannel channel;
        private CountDownLatch latch;

        public TimeClientHandler(String host, int port) {
            this.host = host;
            this.port = port;
            try {
                channel = AsynchronousSocketChannel.open();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {

            latch = new CountDownLatch(1);

            //①与服务器异步连接
            channel.connect(new InetSocketAddress(host, port), this, this);

            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        /**
         * ②与服务器建立连接之后的回调
         *
         * @param result
         * @param attachment
         */
        @Override
        public void completed(Void result, TimeClientHandler attachment) {
            byte[] request = "query time".getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(request.length);
            writeBuffer.put(request);
            writeBuffer.flip();
            //③异步向服务器发送消息
            channel.write(writeBuffer, writeBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                /**
                 *
                 * ④向服务器发送消息成功之后的回调
                 *
                 * @param result
                 * @param attachment
                 */
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    System.out.println("客户端成功向服务器发送查询指令");
                    if (attachment.hasRemaining()) {
                        channel.write(attachment, attachment, this);
                    } else {
                        //⑤异步接收来自服务器的消息
                        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                        channel.read(readBuffer, readBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                            /**
                             *
                             * ⑥成功接收到来自服务器的消息之后的回调
                             *
                             * @param result
                             * @param attachment
                             */
                            @Override
                            public void completed(Integer result, ByteBuffer attachment) {
                                attachment.flip();
                                byte[] bytes = new byte[attachment.remaining()];
                                attachment.get(bytes);
                                try {
                                    String response = new String(bytes, "UTF-8");
                                    System.out.println("客户端接收到服务器发送的消息：" + response);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                } finally {
                                    latch.countDown();
                                }
                            }

                            @Override
                            public void failed(Throwable exc, ByteBuffer attachment) {
                                try {
                                    channel.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } finally {
                                    latch.countDown();
                                }
                            }
                        });
                    }
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    try {
                        channel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        @Override
        public void failed(Throwable exc, TimeClientHandler attachment) {
            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }
    }
}
