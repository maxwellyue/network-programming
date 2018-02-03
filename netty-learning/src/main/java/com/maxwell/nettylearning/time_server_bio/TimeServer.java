package com.maxwell.nettylearning.time_server_bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

/************************************************************************************
 * 功能描述：
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
            while (true) {
                socket = server.accept();
                //处理连接：每来一个
                new Thread(new TimeServerHandler(socket)).start();
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

