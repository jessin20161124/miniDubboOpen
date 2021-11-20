package com.jessin.practice.dubbo.processor;

import com.jessin.practice.dubbo.config.InterfaceConfig;
import com.jessin.practice.dubbo.config.MiniDubboProperties;
import java.lang.reflect.Field;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;

/**
 * 接口可以直接代理掉，而不需要具体的实现类，包括dubbo/mybatis mapper
 * @Author: jessin
 * @Date: 19-11-25 下午9:49
 */
@Slf4j
public class ReferenceBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter {
    private MiniDubboProperties miniDubboProperties;
    public ReferenceBeanPostProcessor(MiniDubboProperties miniDubboProperties) {
        this.miniDubboProperties = miniDubboProperties;
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
            throws BeansException {
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            boolean isExist = field.isAnnotationPresent(Reference.class);
            if (isExist) {
                try {
                    if (!field.getType().isInterface()) {
                        throw new RuntimeException("dubbo依赖不是接口：" + field.getType().getName());
                    }
                    Reference  ref = field.getAnnotation(Reference.class);
                    log.info("尝试注入接口代理，bean的{}属性为：{}", beanName, ref);
                    // 私有属性，必须设置为可访问
                    field.setAccessible(true);
                    field.set(bean, JdkDynamicProxy.createProxy(field.getType(), transform(ref), miniDubboProperties));
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
}
