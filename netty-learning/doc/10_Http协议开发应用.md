### Http协议介绍

//todo

---
### Netty Http

由于Netty天生是异步事件驱动的架构，因此基于NIO TCP协议栈开发的Http协议栈也是异步非阻塞的。

Netty的Http协议栈无论在性能还是可靠性上，都表现优异，非常适合在非WEB容器的场景下应用，
相比于传统的Tomcat、Jetty等Web容器，它更加轻量和小巧，灵活性和定制性也更好。


---
### 使用Netty开发一个Http文件服务器

实例开发场景描述：

使用Netty开发一个文件服务器：使用Http对外提供服务，当客户端通过浏览器访问文件服务器时，对访问路径进行检查，

检查失败时返回403，该页无法访问；如果校验通过，以链接的方式打开当前文件夹目录，每个目录或者文件都是一个超链接，
可以递归访问。

如果是目录，可以继续递归访问它下面的子目录或文件，如果是文件且可读，则可以在浏览器直接打开，或者可以下载文件。


代码目录：

`http-file-server`



---

### 使用Netty + Json开发Restful API

代码目录：

`http-server-json`








