package com.maxwell.nettylearning.websocket_server_chat;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yueyuemax@gmail.com
 * 创建时间： 2018年02月05日 --  下午6:29 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class ChannelGroupEnum {

    private ChannelGroupEnum(){}

    private static class ChannelGroupHolder{
        private static final ChannelGroup INSTANCE = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    }

    public static final ChannelGroup channelGroup(){
        return ChannelGroupHolder.INSTANCE;
    }
}
