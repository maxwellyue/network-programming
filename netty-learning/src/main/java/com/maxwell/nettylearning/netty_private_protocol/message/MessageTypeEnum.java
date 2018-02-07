package com.maxwell.nettylearning.netty_private_protocol.message;

/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yueyuemax@gmail.com
 * 创建时间： 2018年02月06日 --  上午10:37 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public enum MessageTypeEnum {

    BUS_REQUEST((byte) 0, "业务请求消息"),

    BUS_RESPONSE((byte)1, "业务响应消息"),

    ONE_WAY((byte)2, "既是请求又是响应消息"),

    HAND_SHAKER_REQUEST((byte)3, "握手请求消息"),

    HAND_SHAKER_RESPONSE((byte)4, "握手响应消息"),

    HEART_REQUEST((byte)5, "心跳请求消息"),

    HEART_RESPONSE((byte)6, "心跳响应消息");

    public byte value;
    private String desc;

    private MessageTypeEnum(byte value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
