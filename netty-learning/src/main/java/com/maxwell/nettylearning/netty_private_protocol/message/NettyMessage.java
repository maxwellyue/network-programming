package com.maxwell.nettylearning.netty_private_protocol.message;

/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yueyuemax@gmail.com
 * 创建时间： 2018年02月06日 --  上午10:30 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public final class NettyMessage {

    /**
     * 消息头
     */
    private MessageHeader header;

    /**
     * 消息体
     */
    private Object body;


    public MessageHeader getHeader() {
        return this.header;
    }

    public void setHeader(MessageHeader header) {
        this.header = header;
    }

    public Object getBody() {
        return this.body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "NettyMessage{" +
                "header=" + header.toString() +
                ", body=" + (body == null ? "" : body.toString()) +
                '}';
    }
}
