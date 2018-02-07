### 引入Netty
在pom文件中添加以下内容即可：
```
<!-- https://mvnrepository.com/artifact/io.netty/netty-all -->
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-all</artifactId>
    <version>5.0.0.Alpha2</version>
</dependency>
```

### 流程图

服务端：

![服务端创建流程](https://github.com/maxwellyue/network-programming/blob/master/netty-learning/doc/images/Netty%E6%9C%8D%E5%8A%A1%E7%AB%AF%E5%88%9B%E5%BB%BA%E6%B5%81%E7%A8%8B.png?raw=true)

客户端：

![客户端端创建流程](https://github.com/maxwellyue/network-programming/blob/master/netty-learning/doc/images/Netty%E5%AE%A2%E6%88%B7%E7%AB%AF%E5%88%9B%E5%BB%BA%E6%B5%81%E7%A8%8B.png?raw=true)


---
代码对应目录：

`time_server_netty`

