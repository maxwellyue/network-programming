###  WebSocket简介

WebSocket是一种在单个TCP连接上进行全双工通讯的协议。WebSocket通信协议于2011年被IETF定为标准RFC 6455，并由RFC7936补充规范。WebSocket API也被W3C定为标准。

WebSocket使得客户端和服务器之间的数据交换变得更加简单，允许服务端主动向客户端推送数据。在WebSocket API中，浏览器和服务器只需要完成一次握手，两者之间就直接可以创建持久性的连接，并进行双向数据传输。

##### 背景

现在，很多网站为了实现推送技术，所用的技术都是轮询。轮询是在特定的的时间间隔（如每1秒），由浏览器对服务器发出HTTP请求，然后由服务器返回最新的数据给客户端的浏览器。这种传统的模式带来很明显的缺点，即浏览器需要不断的向服务器发出请求，然而HTTP请求可能包含较长的头部，其中真正有效的数据可能只是很小的一部分，显然这样会浪费很多的带宽等资源。

而比较新的技术去做轮询的效果是Comet。这种技术虽然可以双向通信，但依然需要反复发出请求。而且在Comet中，普遍采用的长链接，也会消耗服务器资源。

在这种情况下，HTML5定义了WebSocket协议，能更好的节省服务器资源和带宽，并且能够更实时地进行通讯。

`WebSocket`使用`ws`或`wss`的统一资源标志符，类似于`HTTPS`，其中`wss`表示在`TLS`之上的`WebSocket`。如：
```
ws://example.com/wsapi
wss://secure.example.com/
```


`WebSocket`使用和`HTTP`相同的`TCP`端口，可以绕过大多数防火墙的限制。

默认情况下，`WebSocket`协议使用`80`端口；运行在`TLS`之上时，默认使用`443`端口。

##### 优点

* 较少的控制开销。
<br>在连接创建后，服务器和客户端之间交换数据时，用于协议控制的数据包头部相对较小。在不包含扩展的情况下，对于服务器到客户端的内容，此头部大小只有2至10字节（和数据包长度有关）；对于客户端到服务器的内容，此头部还需要加上额外的4字节的掩码。相对于HTTP请求每次都要携带完整的头部，此项开销显著减少了。
* 更强的实时性。
<br>由于协议是全双工的，所以服务器可以随时主动给客户端下发数据。相对于`HTTP`请求需要等待客户端发起请求服务端才能响应，延迟明显更少；即使是和`Comet`等类似的长轮询比较，其也能在短时间内更多次地传递数据。
* 保持连接状态。
<br>于HTTP不同的是，`WebSocket`需要先创建连接，这就使得其成为一种有状态的协议，之后通信时可以省略部分状态信息。而`HTTP`请求可能需要在每个请求都携带状态信息（如身份认证等）。
* 更好的二进制支持。
<br>`WebSocket`定义了二进制帧，相对`HTTP`，可以更轻松地处理二进制内容。
* 可以支持扩展。
<br>`WebSocket`定义了扩展，用户可以扩展协议、实现部分自定义的子协议。如部分浏览器支持压缩等。
* 更好的压缩效果。
<br>相对于HTTP压缩，`WebSocket`在适当的扩展支持下，可以沿用之前内容的上下文，在传递类似的数据时，可以显著地提高压缩率。


##### 握手协议

`WebSocket`是独立的、创建在`TCP`上的协议。

`WebSocket` 通过`HTTP/1.1`协议的101状态码进行握手。

为了创建`WebSocket`连接，需要通过浏览器发出请求，之后服务器进行回应，这个过程通常称为“握手”（`handshaking`）。

一个典型的`WebSocket`握手请求如下：

客户端请求
```
GET / HTTP/1.1
Upgrade: WebSocket
Connection: Upgrade
Host: example.com
Origin: http://example.com
Sec-WebSocket-Key: sN9cRrP/n9NdMgdcy2VJFQ==
Sec-WebSocket-Version: 13
```

服务器回应
```
HTTP/1.1 101 Switching Protocols
Upgrade: WebSocket
Connection: Upgrade
Sec-WebSocket-Accept: fFBooB7FAkLlXgRSz0BT3v4hq5s=
Sec-WebSocket-Location: ws://example.com/
```
* 字段说明
  * `Connection`必须设置`Upgrade`，表示客户端希望连接升级。
  * `Upgrade`字段必须设置`WebSocket`，表示希望升级到`WebSocket`协议。
  * `Sec-WebSocket-Key`是随机的字符串，服务器端会用这些数据来构造出一个`SHA-1`的信息摘要。把`Sec-WebSocket-Key`加上一个特殊字符串“258EAFA5-E914-47DA-95CA-C5AB0DC85B11”，然后计算SHA-1摘要，之后进行BASE-64编码，将结果做为“Sec-WebSocket-Accept”头的值，返回给客户端。如此操作，可以尽量避免普通`HTTP`请求被误认为`WebSocket`协议。
  * `Sec-WebSocket-Version`表示支持的`WebSocket`版本。`RFC6455`要求使用的版本是`13`，之前草案的版本均应当弃用。
  * `Origin`字段是可选的，通常用来表示在浏览器中发起此`WebSocket`连接所在的页面，类似于`Referer`。但是，与`Referer`不同的是，`Origin`只包含了协议和主机名称。

其他一些定义在`HTTP`协议中的字段，如`Cookie`等，也可以在`WebSocket`中使用。

##### 浏览器
实现`WebSocket`的协议，浏览器扮演着一个很重要的角色。所有最新的浏览器支持最新规范`（RFC 6455）`的`WebSocket`协议。


----

### 使用Netty开发WebSocket服务端


代码目录

`websocket_server`：建立连接、相互发送消息、连接关闭的演示

`websocket_server_push`：服务端向客户端发送定时消息

`websocket_server_chat`：简单聊天室的实现

