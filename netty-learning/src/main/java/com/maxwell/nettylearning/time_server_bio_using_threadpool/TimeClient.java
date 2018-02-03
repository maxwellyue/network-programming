package com.maxwell.nettylearning.time_server_bio_using_threadpool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yuezc@seentao.com
 * 创建时间： 2018年02月02日 --  下午7:08 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class TimeClient {

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 8080;

        try (
                Socket socket = new Socket(host, port);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        ) {
            //向服务器发送消息
            out.println("query time");
            //接收服务器发来的消息
            String response = in.readLine();
            System.out.println("从时间服务器得知，现在时间是：" + response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
