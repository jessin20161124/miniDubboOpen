package com.jessin.practice.dubbo.registry;

import com.google.common.collect.Lists;
import com.jessin.practice.dubbo.config.InterfaceConfig;
import com.jessin.practice.dubbo.invoker.DubboInvoker;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 *  * 注册项目录，所有的dubboInvoker都保存到这里，实现zk listener，监听zk路径变化，当zk变化时，构造DubboInvoker。
 *  * 每个service应该有一个，同一个zk集群同一台机器应该只建立一个共享连接
 * @Author: jessin
 * @Date: 19-11-25 下午10:34
 */
@Slf4j
public class RegistryDirectory implements ChildListener {

    private Map<String, DubboInvoker> ipAndPort2InvokerMap = new ConcurrentHashMap<>();

    private RegistryService registryService;

    private InterfaceConfig interfaceConfig;

    private String providerPath;

    /**
     * 创建zk连接，监听zk路径创建DubboInvoker
     * @param path
     */
    public RegistryDirectory(String path, String registry, InterfaceConfig interfaceConfig) {
        this.interfaceConfig = interfaceConfig;
        // 监听group/接口名/providers，有变化时通知RegistryDirectory，也就是调用notify(url, listener, urls);
        this.providerPath = "/miniDubbo/" + interfaceConfig.getGroup() + "/" + path + "/providers";

        // 判断zk/redis。创建zk连接，并创建RegistryDirectory，第一次时创建DubboInvoker
        registryService = RegistryManager.getRegistryService(registry);
        registryService.subscribe(providerPath, this);
    }

    /**
     * 注意，这里需要try/catch，否则抛异常后不会再有通知
     * @param path
     * @param children
     */
    @Override
    public synchronized void childChanged(String path, List<String> children) {
        log.info("监听到zk路径变化:{}，children:{}", path, children);
        try {
            if (children == null || children.size() == 0) {
                // 可能是远程抖动，或者zookeeper出问题了，造成所有服务实例下线，这里还需要通过心跳检测。
                log.info("监听到zk路径无子节点:{}", providerPath);
                children = Lists.newArrayList();
            }
            List<String> added = children.stream()
                    .filter(one -> !ipAndPort2InvokerMap.containsKey(one))
                    .collect(Collectors.toList());
            List<String> finalChildren = children;
            List<String> deleted = ipAndPort2InvokerMap.keySet().stream()
                    .filter(one -> !finalChildren.contains(one))
                    .collect(Collectors.toList());
            log.info("监听到zk路径：{}，子节点变化，新增zk节点：{}，删除zk节点：{}", providerPath, added, deleted);

            added.forEach(ipAndPort -> {
                if (!ipAndPort2InvokerMap.containsKey(ipAndPort)) {
                    ipAndPort2InvokerMap.put(ipAndPort, new DubboInvoker(ipAndPort, interfaceConfig));
                }
            });
            deleted.forEach(ipAndPort -> {
                ipAndPort2InvokerMap.get(ipAndPort).destroy();
                ipAndPort2InvokerMap.remove(ipAndPort);
            });
        } catch (Exception e) {
            log.error("处理zk事件出错", e);
        }
    }

    public List<DubboInvoker> getInvokerList() {
        // 找出可用的，zk有子节点，不表示节点底层网络可连通，例如zk超时时间比较长，还没有检测到
        return ipAndPort2InvokerMap.values().stream().filter(dubboInvoker -> {
            boolean available = dubboInvoker.isAvailable();
            if (!available) {
                log.warn("zk上该节点存在，但是底层网络探测到已经断开：{}", dubboInvoker.getIpAndPort());
            }
            return available;
        }).collect(Collectors.toList());
    }
}
