package com.jessin.practice.dubbo.spring.processor;

import com.jessin.practice.dubbo.config.ServiceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 *  spring托管
 * @Author: jessin
 * @Date: 19-11-27 下午10:31
 */
@Slf4j
public class ServiceBean extends ServiceConfig implements SmartInitializingSingleton, DisposableBean {

    /**
     * 在InitializingBean.afterPropertiesSet阶段注册zk有问题，可能导致流量过早过来，但是其他bean还未初始化
     * 这里到迁移到容器启动后，所有单例都初始化完毕后再注册，
     * SmartInitializingSingleton.afterSingletonsInstantiated
     * 或者监听ContextRefreshedEvent
     *
     */
    @Override
    public void afterSingletonsInstantiated() {
        export();
    }

    @Override
    public void destroy() throws Exception {
        unexport();
    }
}
