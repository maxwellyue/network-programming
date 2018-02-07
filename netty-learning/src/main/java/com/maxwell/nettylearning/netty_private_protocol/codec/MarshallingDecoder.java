package com.maxwell.nettylearning.netty_private_protocol.codec;

import io.netty.buffer.ByteBuf;
import org.jboss.marshalling.Unmarshaller;

import java.io.IOException;

/**
 * 编码类
 * @author lsq 
 */
public class MarshallingDecoder {
	
	private final Unmarshaller unmarshaller;
	
	public MarshallingDecoder() throws IOException {
		unmarshaller = MarshallingCodecFactory.buildUnMarshalling();
	}
	
	protected Object decode(ByteBuf in) throws Exception {
		//先取出消息长度
		int objectSize = in.readInt();
		//跳过消息长度
		ByteBuf buf = in.slice(in.readerIndex(), objectSize);
		ChannelBufferByteInput input = new ChannelBufferByteInput(buf);
		try {
			//开始从stream中读取object
			unmarshaller.start(input);
			Object obj = unmarshaller.readObject();
			unmarshaller.finish();
			in.readerIndex(in.readerIndex() + objectSize);
			return obj;
		} finally {
			unmarshaller.close();
		}
	}
}
