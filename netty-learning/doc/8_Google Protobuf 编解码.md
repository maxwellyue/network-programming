### Protobuf使用

按照下面步骤来：

①在`pom`文件中，添加如下内容：
```
<properties>
    <grpc.version>1.6.1</grpc.version>
    <protobuf.version>3.3.0</protobuf.version>
</properties>

<dependencies>
    <!--protobuf生成Java类-->
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-netty</artifactId>
        <version>${grpc.version}</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-protobuf</artifactId>
        <version>${grpc.version}</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-stub</artifactId>
        <version>${grpc.version}</version>
        <scope>provided</scope>
    </dependency>

    <!--protobuf-->
    <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java</artifactId>
        <version>${protobuf.version}</version>
    </dependency>

</dependencies>



<build>
    ...
    <extensions>
        <extension>
            <groupId>kr.motd.maven</groupId>
            <artifactId>os-maven-plugin</artifactId>
            <version>1.5.0.Final</version>
        </extension>
    </extensions>
    <plugins>
        <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>0.5.0</version>
            <configuration>
                <protocArtifact>com.google.protobuf:protoc:${protobuf.version}:exe:${os.detected.classifier}</protocArtifact>
                <pluginId>grpc-java</pluginId>
                <pluginArtifact>io.grpc:protoc-gen-grpc-java:${grpc.version}:exe:${os.detected.classifier}</pluginArtifact>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                        <goal>compile-custom</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
    ...
</build>


```

②在项目`src`下新建`proto`包，在里面编写`protobuf`文件

如果使用的是`IntelliJ IDEA`，可以安装`protobuf support`插件，这样在编写`protobuf`文件的时候会有语法高亮，有错误也会提示。

例如：
```
syntax = "proto3";
package com.maxwell.nettylearning.subreq_using_protobuf;
option java_outer_classname = "SubscribeReqProto";

message SubscribeReq {
    int32 subReqID = 1;
    string username = 2;
    string productName = 3;
    repeated string address = 4;
}
```

③执行`mvn compile`，生成`proto`文件对应的`Java`类：在`target/generated-sources-protobuf/java`中，将生成的`Java`类`Copy`到项目中即可。


---
### Netty中使用Protobuf

首先，要完成上述"Protobuf使用"中描述的工作。

在服务器端和客户端的ChannelPipe中添加对应的编解码器：

```
ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
//ProtobufDecoder中需要解码的类，需要解码哪个就设置为哪个
ch.pipeline().addLast(new ProtobufDecoder(SubscribeReqProto.SubscribeReq.getDefaultInstance()));
ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
ch.pipeline().addLast(new ProtobufEncoder());
```


---

对应的代码目录

`subreq_using_protobuf`
