### 已完成功能列表
- [x] 基于zk服务注册和服务发现
- [x] 基于netty + json序列化网络通信
- [x] zk连接复用、netty client连接复用
- [x] 与spring boot集成


### TODO LIST
&nbsp;&nbsp;&nbsp;通用需求：
- [ ] spring容器销毁时，关闭占用的资源，如netty client/netty server
- [ ] 支持protobuf序列化
- [ ] 支持http协议通信
- [ ] netty超时重连，心跳断开重连，销毁dubbo invoker
- [ ] zk抖动导致所有服务实例下线优化
- [ ] 其他注册中心支持，如consul/redis
- [ ] Attach/tag实现
- [ ] 支持一个类多个版本实现
- [ ] zk重新连接时，需要重新注册关注的事件，恢复现场，对于服务端是重新注册；对于客户端是重新订阅


&nbsp;&nbsp;&nbsp;provider功能：
- [ ] 服务端优雅启动和优雅下线，防止流量过早进来，造成超时。在spring容器启动成功后，再注册到zk上。在spring容器销毁时，先从zk取消注册，最后再关闭客户端连接。
- [ ] 服务端请求用线程池实现，避免阻塞NioEventLoop
- [ ] 服务端支持曝光实现多个接口的一个类


&nbsp;&nbsp;&nbsp;consumer功能：
- [ ] netty client通过计数引用销毁资源
- [ ] 服务负载均衡算法：随机/轮询/加权
- [ ] 集群失败策略：failover/failsafe/failback

### 博客参考：
[简易版dubbo实现](https://blog.csdn.net/ac_dao_di/article/details/121445493)

### 其他工程：
[api](https://github.com/jessin20161124/api)
[miniDubboDemo](https://github.com/jessin20161124/miniDubboDemo)

### 更多精彩样例，请关注公众号：
![扫一扫](https://img-blog.csdnimg.cn/e021faa547534e0080356b65d995b6f8.png?x-oss-process=image/watermark,type_ZHJvaWRzYW5zZmFsbGJhY2s,shadow_50,text_Q1NETiBAYWNfZGFvX2Rp,size_20,color_FFFFFF,t_70,g_se,x_16#pic_center)


