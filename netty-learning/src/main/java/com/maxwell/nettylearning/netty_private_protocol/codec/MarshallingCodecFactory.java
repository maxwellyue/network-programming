package com.maxwell.nettylearning.netty_private_protocol.codec;

import org.jboss.marshalling.*;

import java.io.IOException;

/**
 * 工厂类
 * @author lsq 
 */
public class MarshallingCodecFactory {

	/**
	 * 创建Jboss Marshaller 
	 * @throws IOException 
	 */
	protected static Marshaller buildMarshalling() throws IOException {
		//获取MarshallerFactory实例,serial表示创建的是java序列化工厂对象
		final MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory("serial");
		final MarshallingConfiguration configuration = new MarshallingConfiguration();
		configuration.setVersion(5);
		Marshaller marshaller = marshallerFactory.createMarshaller(configuration);
		return marshaller;
	}
	
	/**
	 * 创建Jboss Unmarshaller 
	 */
	protected static Unmarshaller buildUnMarshalling() throws IOException {
		final MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory("serial");
		final MarshallingConfiguration configuration = new MarshallingConfiguration();
		configuration.setVersion(5);
		final Unmarshaller unmarshaller = marshallerFactory.createUnmarshaller(configuration);
		return unmarshaller;
    } 
}
