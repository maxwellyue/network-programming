
`Netty`中的`ChannelHandler`和`ChannelPipeline`的概念，可以类比于`Java Web`中的`Servlet`和`Filter`的概念。

Netty将`Channel`中数据管道抽象为`ChannelPipeline`，消息在`ChannelPipeline`中流动和传递。

`ChannelPipeline`持有IO事件拦截器`ChannelHandler`的链表，由`ChannelHandler`对IO事件进行拦截和处理，
可以通过新增和删除`ChannelHandler`来实现不同的业务逻辑定制，不需要对已有的`ChannelHandler`进行修改。


---
### `ChannelPipeline`

`ChannelPipeline`是`ChannelHandler`的容器，它负责`ChannelHandler`的管理和事件拦截与调度。



