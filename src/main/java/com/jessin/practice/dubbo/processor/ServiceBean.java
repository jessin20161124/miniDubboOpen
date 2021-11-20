package com.jessin.practice.dubbo.processor;

import com.jessin.practice.dubbo.config.InterfaceConfig;
import com.jessin.practice.dubbo.config.MiniDubboProperties;
import com.jessin.practice.dubbo.exporter.DubboExporter;
import com.jessin.practice.dubbo.netty.NettyManager;
import com.jessin.practice.dubbo.netty.NettyServer;
import com.jessin.practice.dubbo.registry.CuratorZookeeperClient;
import com.jessin.practice.dubbo.registry.RegistryManager;
import com.jessin.practice.dubbo.utils.NetUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * @Author: jessin
 * @Date: 19-11-27 下午10:31
 */
@Slf4j
public class ServiceBean implements InitializingBean, DisposableBean {

    private NettyServer nettyServer;
    /**
     * zk地址
     */
    private CuratorZookeeperClient curatorZookeeperClient;

    private Object ref;

    private MiniDubboProperties miniDubboProperties;

    private InterfaceConfig interfaceConfig;

    public MiniDubboProperties getMiniDubboProperties() {
        return miniDubboProperties;
    }

    public void setMiniDubboProperties(MiniDubboProperties miniDubboProperties) {
        this.miniDubboProperties = miniDubboProperties;
    }

    public InterfaceConfig getInterfaceConfig() {
        return interfaceConfig;
    }

    public void setInterfaceConfig(InterfaceConfig interfaceConfig) {
        this.interfaceConfig = interfaceConfig;
    }

    public Object getRef() {
        return ref;
    }

    public void setRef(Object ref) {
        this.ref = ref;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Class[] interfaces = ref.getClass().getInterfaces();
        if (interfaces.length <= 0) {
            throw new IllegalStateException(ref.getClass().getName() + "未实现接口");
        }
        // todo 目前只能实现一个接口
        String clazzName = interfaces[0].getName();
        log.info("曝光key：{}，ref：{}", clazzName, ref);
        // 暴露服务 todo 版本
        DubboExporter.exportService(clazzName, ref);
        // 先开启，再注册
        // 判断协议
        if ("dubbo".equals(miniDubboProperties.getProtocol())) {
            // 开启netty server
            nettyServer = NettyManager.getNettyServer(miniDubboProperties.getPort());
        } else {
            throw new RuntimeException("unknown communicate protocol:" + miniDubboProperties.getProtocol());
        }
        // 判断什么类型的注册中心
        curatorZookeeperClient = RegistryManager.getCuratorZookeeperClient(miniDubboProperties.getRegistry());
        String providerPath = "/miniDubbo/" + interfaceConfig.getGroup() + "/" + clazzName + "/providers" + "/" + NetUtils.getServerIp() + ":" + miniDubboProperties.getPort();

        // 注册zk，提炼register方法
        curatorZookeeperClient.create(providerPath, true);
    }

    @Override
    public void destroy() throws Exception {
        curatorZookeeperClient.doClose();
        nettyServer.close();
    }
}
