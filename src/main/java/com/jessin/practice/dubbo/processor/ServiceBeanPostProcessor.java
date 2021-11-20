package com.jessin.practice.dubbo.processor;

import com.jessin.practice.dubbo.config.InterfaceConfig;
import com.jessin.practice.dubbo.config.MiniDubboProperties;
import java.util.Set;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

/**
 * @Author: jessin
 * @Date: 19-11-27 下午9:24
 */
public class ServiceBeanPostProcessor implements BeanDefinitionRegistryPostProcessor, BeanClassLoaderAware {

    private AnnotationBeanNameGenerator annotationBeanNameGenerator = new AnnotationBeanNameGenerator();

    private MiniDubboProperties miniDubboProperties;

    private ClassLoader classLoader;

    public ServiceBeanPostProcessor(MiniDubboProperties miniDubboProperties) {
        this.miniDubboProperties = miniDubboProperties;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        ClassPathBeanDefinitionScanner classPathBeanDefinitionScanner = new ClassPathBeanDefinitionScanner(registry, false);
        classPathBeanDefinitionScanner.addIncludeFilter(new AnnotationTypeFilter(Service.class));
        // 将该包下的@Service注解全部扫描为bean
        Set<BeanDefinition> beanDefinitionSet
                = classPathBeanDefinitionScanner.findCandidateComponents(miniDubboProperties.getPackagePath());
        for (BeanDefinition beanDefinition : beanDefinitionSet) {
            String beanName = annotationBeanNameGenerator.generateBeanName(beanDefinition, registry);
            registry.registerBeanDefinition(beanName, beanDefinition);
            BeanDefinition wrapper = new RootBeanDefinition(ServiceBean.class);
            wrapper.getPropertyValues().addPropertyValue("ref", new RuntimeBeanReference(beanName));
            wrapper.getPropertyValues().addPropertyValue("miniDubboProperties", miniDubboProperties);
            Class beanClass = ClassUtils.resolveClassName(beanDefinition.getBeanClassName(), classLoader);
            Service service = AnnotationUtils.findAnnotation(beanClass, Service.class);
            wrapper.getPropertyValues().addPropertyValue("interfaceConfig", transform(service));
            registry.registerBeanDefinition("dubbo_" + beanName, wrapper);
        }
    }


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    private InterfaceConfig transform(Service ref) {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setGroup(ref.group());
        interfaceConfig.setVersion(ref.version());
        interfaceConfig.setTimeout(ref.timeout());
        return interfaceConfig;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
}
