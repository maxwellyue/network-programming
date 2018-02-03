package com.maxwell.nettylearning.time_server_bio_using_threadpool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/************************************************************************************
 * 功能描述：
 *
 *
 * 使用线程池可以避免为每个请求都创建一个独立线程造成的线程资源耗尽问题，
 *
 * 但是由于底层的通信（in.readLine()， out.println（））仍然采用的阻塞模型。
 *
 * 也就是当readLine、println在阻塞的时候，任务所在线程也会阻塞，产生浪费。
 *
 * 创建人：岳增存
 * 创建时间： 2018年02月02日 --  下午7:07 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class TimeServer {

    public static void main(String[] args) {

        int port = 8080;

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("时间服务器已启动，端口为：" + port);

            Socket socket = null;

            ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                    50, 120L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10000));
            while (true) {
                socket = server.accept();
                //每来一个连接，就丢到线程池中处理
                poolExecutor.execute(new TimeServerHandler(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 处理Socket连接
     */
    static class TimeServerHandler implements Runnable {

        private Socket socket;

        public TimeServerHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                    PrintWriter out = new PrintWriter(this.socket.getOutputStream(), true);
            ) {
                String currentTime = null;
                String body = null;
                while (true) {
                    //从客户端接收消息
                    body = in.readLine();
                    if (body == null) {
                        break;
                    } else {
                        System.out.println("时间服务器收到来自客户端的消息：" + body);
                        currentTime = "query time".equalsIgnoreCase(body) ? new Date().toString() : "bad order";
                        //向客户端发送消息
                        out.println(currentTime);
                        System.out.println("时间服务器已向客户端发送消息：" + currentTime);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

