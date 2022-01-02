### 已完成功能列表
- [x] 基于zk服务注册和服务发现
- [x] 基于netty + json序列化网络通信
- [x] zk连接复用、netty client连接复用
- [x] 与spring boot集成
- [x] 支持一个类多个版本实现
- [x] 服务端优雅启动，防止流量过早进来，造成超时。在spring容器启动成功后，再注册到zk上。
- [x] zk重新连接时，重新注册关注的事件，恢复现场。对于服务端是重新注册；对于客户端是重新订阅
- [x] netty心跳保活机制，客户端超时断开重连，重连时dubbo invoker不可用，服务端超时关闭无效连接
- [x] 服务端请求用线程池实现，避免阻塞NioEventLoop


### TODO LIST
&nbsp;&nbsp;&nbsp;&nbsp;通用需求：
- [ ] shutdown hook。spring容器销毁时，关闭占用的资源，如netty client/netty server
- [ ] zk抖动导致所有服务实例下线优化
- [ ] 支持protobuf序列化
- [ ] 支持http协议通信
- [ ] 其他注册中心支持，如consul/redis
- [ ] Attach/tag实现

&nbsp;&nbsp;&nbsp;&nbsp;provider功能：
- [ ] 服务端优雅下线。在spring容器销毁前(ContextClosedEvent)，先从zk取消注册，最后再关闭客户端连接。
- [ ] 服务端支持曝光实现多个接口的一个类

&nbsp;&nbsp;&nbsp;&nbsp;consumer功能：
- [ ] netty client通过计数引用销毁资源
- [ ] 服务负载均衡算法：随机/轮询/加权，服务同机房路由
- [ ] 集群失败策略：failover/failsafe/fallback

&nbsp;&nbsp;&nbsp;&nbsp;控制台规划：
- [ ] 服务展示和治理
- [ ] 支持管理服务上下线
- [ ] 支持动态配置和下发

### 博客参考：
[简易版dubbo实现](https://blog.csdn.net/ac_dao_di/article/details/121445493)

### 其他工程：
[api](https://github.com/jessin20161124/api)
[miniDubboDemo](https://github.com/jessin20161124/miniDubboDemo)

### 更多精彩样例，请关注公众号：
![扫一扫](https://img-blog.csdnimg.cn/e021faa547534e0080356b65d995b6f8.png?x-oss-process=image/watermark,type_ZHJvaWRzYW5zZmFsbGJhY2s,shadow_50,text_Q1NETiBAYWNfZGFvX2Rp,size_20,color_FFFFFF,t_70,g_se,x_16#pic_center)


