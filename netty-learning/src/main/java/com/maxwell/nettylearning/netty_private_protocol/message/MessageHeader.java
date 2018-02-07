package com.maxwell.nettylearning.netty_private_protocol.message;

import java.util.HashMap;
import java.util.Map;

/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yueyuemax@gmail.com
 * 创建时间： 2018年02月06日 --  上午10:30 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public final class MessageHeader {

    /**
     * Netty消息的校验码，它由三部分组成
     * 1）0xABEF：固定值，表明该消息是Netty协议消息
     * 2）主版本号：1~255，1个字节
     * 3）次版本号：1~255，1个字节
     *
     * crcCode = 0xABEF + 主版本号 + 次版本号
     *
     * 32位，即4个字节
     *
     */
    private int crcCode = 0xABEF0101;
    /**
     *
     * 消息长度，包括消息头和消息体，即消息的总长度
     *
     * 32位，即4个字节
     *
     */
    private int length;

    /**
     * 集群节点内全局唯一，由会话ID生成器生成
     *
     * 64位，即8个字节
     */
    private long sessionId;

    /**
     *
     * 消息类型：1个字节
     *
     */
    private Byte type;

    /**
     * 消息优先级：0~255，
     *
     * 1个字节
     *
     */
    private Byte priority;

    /**
     * 可选字段，用于扩展消息头
     */
    private Map<String, Object> attachment = new HashMap<>();


    public int getCrcCode() {
        return this.crcCode;
    }

    public void setCrcCode(int crcCode) {
        this.crcCode = crcCode;
    }

    public int getLength() {
        return this.length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public long getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public Byte getType() {
        return this.type;
    }

    public void setType(Byte type) {
        this.type = type;
    }

    public Byte getPriority() {
        //优先级默认为1
        return this.priority == null ? (byte) 1 : this.priority;
    }

    public void setPriority(Byte priority) {
        this.priority = priority;
    }

    public Map<String, Object> getAttachment() {
        return this.attachment;
    }

    public void setAttachment(Map<String, Object> attachment) {
        this.attachment = attachment;
    }

    @Override
    public String toString() {
        return "MessageHeader{" +
                "crcCode=" + crcCode +
                ", length=" + length +
                ", sessionId=" + sessionId +
                ", type=" + type +
                ", priority=" + priority +
                ", attachment=" + attachment +
                '}';
    }
}
