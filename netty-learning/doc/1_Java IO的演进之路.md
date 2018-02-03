
### Linux网络I/O模型

根据UNIX网络编程对I/O模型的分类，UNIX提供了5种I/O模型。

* 阻塞I/O模型
* 非阻塞I/O模型
* I/O复用模型
* 信号驱动I/O模型
* 异步I/O模型

Java NIO中的核心类库多路复用器Selector是基于epoll的多路复用技术实现。
>Linux中，epoll是事件驱动，而select/poll则是顺序扫描，且支持的fd（文件描述符）有限。


---
### Linix中的IO多路复用技术

IO多路复用技术是指：将多个IO的阻塞复用到同一个select的阻塞上，从而使系统在单线程的情况下，
可以同时处理多个客户端请求。

epoll相比select作了很多重大改进：

* 支持一个进程打开的socket fd 不受限制，仅受限于操作系统的最大文件句柄数
* IO效率不会随fd数目的增加而线性下降（epoll实现了伪AIO）
* 使用mmap加速内核与用户空间的消息传递
* API更加简单

---

### Java 的IO演进
###### JDK1.0~JDK1.3：BIO<br>
所有Socket通信都采用了同步阻塞模式（BIO），这种一请求一应答的通信模型简化了应用开发，但牺牲了性能。

###### JDK1.4：NIO<br>
提供了很多进行异步IO的API和类库
  * ByteBuffer：进行异步IO操作的缓冲区
  * Pipe：进行异步IO操作的管道
  * Channel：如ServerSocketChannel和SocketChannel
  * 多种字符集的编码和解码
  * 多路复用器selector
  * 文件通道FileChannel

但对文件的处理能力不足：
  * 没有统一的文件属性（例如读写权限）
  * API能力较弱，比如目录的级联创建和递归遍历，要自己实现
  * 所有的文件操作都是同步阻塞调用，不支持异步文件的读写操作
  * 底层存储系统的一些高级API无法使用

###### JDK1.7：NIO2.0
相比NIO，改进有：
* 能够提供批量获取文件属性的API
* 提供了AIO功能
* 提供了通道功能，包括对配置和多播数据报的支持等