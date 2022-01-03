package com.jessin.practice.dubbo.config;

import com.jessin.practice.dubbo.exporter.DubboExporter;
import com.jessin.practice.dubbo.netty.NettyManager;
import com.jessin.practice.dubbo.netty.NettyServer;
import com.jessin.practice.dubbo.registry.RegistryManager;
import com.jessin.practice.dubbo.registry.RegistryService;
import com.jessin.practice.dubbo.utils.NetUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * 已经与spring解耦，可以单独编程使用
 * @Author: jessin
 * @Date: 2022/1/3 4:58 下午
 */
@Slf4j
public class ServiceConfig {

    private AtomicBoolean exported = new AtomicBoolean();

    private AtomicBoolean unexported = new AtomicBoolean();

    private NettyServer nettyServer;
    /**
     * zk地址
     */
    private RegistryService registryService;

    private Object ref;

    private String clazzName;

    private String providerPath;

    private ApplicationConfig applicationConfig;

    private InterfaceConfig interfaceConfig;

    public ApplicationConfig getApplicationConfig() {
        return applicationConfig;
    }

    public void setApplicationConfig(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
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

    /**
     * 只能导一次
     */
    public void export() {
        if (!exported.compareAndSet(false, true)) {
            return;
        }
        Class[] interfaces = ref.getClass().getInterfaces();
        if (interfaces.length <= 0) {
            throw new IllegalStateException(ref.getClass().getName() + "未实现接口");
        }
        // 目前只能实现一个接口
        clazzName = interfaces[0].getName();
        log.info("spring所有单例都初始化完毕，曝光key：{}，ref：{}", clazzName, ref);
        // 暴露服务  版本
        DubboExporter.exportService(clazzName, interfaceConfig, ref);
        // 先开启，再注册
        // 判断协议
        if ("dubbo".equals(applicationConfig.getProtocol())) {
            // 开启netty server
            nettyServer = NettyManager.getNettyServer(applicationConfig.getPort());
        } else {
            throw new RuntimeException("unknown communicate protocol:" + applicationConfig.getProtocol());
        }
        // 判断什么类型的注册中心
        registryService = RegistryManager.getRegistryService(applicationConfig.getRegistry());
        // 原生dubbo是个url
        providerPath = "/miniDubbo/" + interfaceConfig.getGroup() + "/" + clazzName + "/providers" + "/" + NetUtils
                .getServerIp() + ":" + applicationConfig.getPort();

        // 注册zk
        registryService.register(providerPath);
    }

    /**
     * 逆向操作
     * 引用计数销毁 统统使用RegistryManager.getRegistryService管理
     * @throws Exception
     */
    public void unexport() throws Exception {
        if (!exported.get() || !unexported.compareAndSet(false, true)) {
            return;
        }

        // 需要unregister
        registryService.unregister(providerPath);
        // 减少引用，引用为0再关闭。这两个其实可以不移除，一般占用不会太多，client可能太多，需要移除
        RegistryManager.remove(applicationConfig.getRegistry());
        NettyManager.removeNettyServer(applicationConfig.getPort());
        DubboExporter.remove(clazzName, interfaceConfig);
    }
}
