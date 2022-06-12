package com.jessin.practice.dubbo.spring.processor;

import com.google.common.collect.Maps;
import com.jessin.practice.dubbo.config.InterfaceConfig;
import com.jessin.practice.dubbo.config.ReferenceConfig;
import com.jessin.practice.dubbo.spring.config.MiniDubboProperties;
import java.lang.reflect.Field;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;

/**
 * 接口可以直接代理掉，而不需要具体的实现类，包括dubbo/mybatis mapper
 * @Author: jessin
 * @Date: 19-11-25 下午9:49
 */
@Slf4j
public class ReferenceBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter implements DisposableBean {
    private Map<String, ReferenceConfig> referenceConfigMap = Maps.newHashMap();
    private MiniDubboProperties miniDubboProperties;
    public ReferenceBeanPostProcessor(MiniDubboProperties miniDubboProperties) {
        this.miniDubboProperties = miniDubboProperties;
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
            throws BeansException {
        // 父类中的@Reference呢？？方法是否支持这个注解注入？？如果有aop是否有问题
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            boolean isExist = field.isAnnotationPresent(Reference.class);
            if (isExist) {
                try {
                    if (!field.getType().isInterface()) {
                        throw new RuntimeException("dubbo依赖不是接口：" + field.getType().getName());
                    }
                    Reference ref = field.getAnnotation(Reference.class);
                    log.info("尝试注入接口代理，bean:{} 属性为：{}", beanName, field.getName());
                    // 私有属性，必须设置为可访问
                    field.setAccessible(true);
                    // todo bugfix，这里可能版本不一样，或者超时时间等不一样，不能复用，需要挪到底层去，测试两个相同的@Reference!!
                    //  RegistryDirectory只关注providerPath变化即可，RegistryDirectory可以复用，不要往RegistryDirectory传递interfaceConfig
                    // 需要销毁
                    InterfaceConfig interfaceConfig = transform(ref);
                    // 对于reference完全一样的(group+className，zk路径，version在服务端运行时才会校验)，缓存一份就可以了
                    String refKey = field.getType().getName() + "_" + interfaceConfig.getGroup();
                    ReferenceConfig referenceConfig = referenceConfigMap.computeIfAbsent(refKey,
                            key -> new ReferenceConfig(field.getType(), interfaceConfig, miniDubboProperties));
                    field.set(bean, referenceConfig.getProxy());
                } catch (IllegalAccessException e) {
                    log.error("设置jdk实例出错啦：{}", field);
                }
            }
        }
        return pvs;
    }

    private InterfaceConfig transform(Reference ref) {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setGroup(ref.group());
        interfaceConfig.setVersion(ref.version());
        interfaceConfig.setTimeout(ref.timeout());
        interfaceConfig.setFailStrategy(ref.failStrategy());
        interfaceConfig.setRetryCount(ref.retryCount());
        return interfaceConfig;
    }

    @Override
    public void destroy() throws Exception {
        referenceConfigMap.forEach((key, referenceConfig) -> {
            referenceConfig.destroy();
        });
        referenceConfigMap.clear();
    }
}
