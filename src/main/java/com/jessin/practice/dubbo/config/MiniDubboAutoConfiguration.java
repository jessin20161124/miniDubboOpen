package com.jessin.practice.dubbo.config;

import com.jessin.practice.dubbo.processor.ReferenceBeanPostProcessor;
import com.jessin.practice.dubbo.processor.Service;
import com.jessin.practice.dubbo.processor.ServiceBeanPostProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * todo 自己调用自己，以及url支持，资源销毁
 * @Author: jessin
 * @Date: 2021/10/26 9:27 PM
 */
@Configuration
@ConditionalOnClass(Service.class)
@EnableConfigurationProperties(MiniDubboProperties.class)
@Slf4j
public class MiniDubboAutoConfiguration {

    static {
        FastJsonConfig.config();
    }
    /**
     * 由于BeanFactoryPostProcessor是提前获取的，这个时候CommonAnnotationBeanPostProcessor还没注册到beanFactory中，
     * serviceBeanPostProcessor注入的属性为空
     */
//    @Autowired
//    private MiniDubboProperties miniDubboProperties;

//    public MiniDubboAutoConfiguration() {
//        log.info("init MiniDubboAutoConfiguration");
//    }

    /**
     *  由于这个Bean是BeanFactoryPostProcessor，提前获取时，
     *  ConfigurationProperties的ConfigurationPropertiesBindingPostProcessor还没注入到beanFactory中，
     *  所以MiniDubboProperties属性没法注入
     *  这里通过environment构造
     * @param environment
     * @return
     */
    @Bean
    @Conditional(ServerCondition.class)
    @ConditionalOnMissingBean
    public static ServiceBeanPostProcessor serviceBeanPostProcessor(Environment environment) {
        MiniDubboProperties miniDubboProperties = getMiniDubboProperties(environment);
        return new ServiceBeanPostProcessor(miniDubboProperties);
    }
    static class ServerCondition extends AnyNestedCondition {

        ServerCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(prefix = MiniDubboProperties.PREFIX, name = "type", havingValue = "both")
        static class HostProperty {

        }

        @ConditionalOnProperty(prefix = MiniDubboProperties.PREFIX, name = "type", havingValue = "server")
        static class JndiNameProperty {

        }

    }

    /**
     * 使用静态方法，防止造成自动化配置实例提前初始化，没有进行增强
     * @param environment
     * @return
     */
    @Bean
    @Conditional(ClientCondition.class)
    @ConditionalOnMissingBean
    public static ReferenceBeanPostProcessor referenceBeanPostProcessor(Environment environment) {
        MiniDubboProperties miniDubboProperties = getMiniDubboProperties(environment);
        return new ReferenceBeanPostProcessor(miniDubboProperties);
    }

    static class ClientCondition extends AnyNestedCondition {

        ClientCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(prefix = MiniDubboProperties.PREFIX, name = "type", havingValue = "both")
        static class HostProperty {

        }

        @ConditionalOnProperty(prefix = MiniDubboProperties.PREFIX, name = "type", havingValue = "client")
        static class JndiNameProperty {

        }

    }


    private static MiniDubboProperties getMiniDubboProperties(Environment environment) {
        MiniDubboProperties miniDubboProperties = Binder.get(environment) //首先要绑定配置器
                //再将属性绑定到对象上
                .bind(MiniDubboProperties.PREFIX, Bindable.of(MiniDubboProperties.class) ).get(); //再获取实例
        return miniDubboProperties;
    }


}
