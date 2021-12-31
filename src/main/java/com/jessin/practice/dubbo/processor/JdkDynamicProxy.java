package com.jessin.practice.dubbo.processor;

import com.jessin.practice.dubbo.config.InterfaceConfig;
import com.jessin.practice.dubbo.config.MiniDubboProperties;
import com.jessin.practice.dubbo.invoker.FailfastClusterInvoker;
import com.jessin.practice.dubbo.invoker.RpcInvocation;
import com.jessin.practice.dubbo.registry.RegistryDirectory;
import com.jessin.practice.dubbo.transport.Response;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import lombok.extern.slf4j.Slf4j;
/**
 * @Author: jessin
 * @Date: 19-11-25 下午9:54
 */
@Slf4j
public class JdkDynamicProxy<T> implements InvocationHandler {

    private String clazzName;

    private Object proxy;

    private RegistryDirectory registryDirectory;

    private FailfastClusterInvoker failfastClusterInvoker;

    private InterfaceConfig interfaceConfig;

    private MiniDubboProperties miniDubboProperties;

    public JdkDynamicProxy(Class<T> clazz, InterfaceConfig interfaceConfig, MiniDubboProperties miniDubboProperties) {
        proxy = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{clazz}, this);
        this.clazzName = clazz.getName();
        registryDirectory = new RegistryDirectory(clazzName, miniDubboProperties.getRegistry(), interfaceConfig);
        failfastClusterInvoker = new FailfastClusterInvoker(registryDirectory);
        this.interfaceConfig = interfaceConfig;
    }


    public static <T>  Object createProxy(Class<T> clazz, InterfaceConfig interfaceConfig, MiniDubboProperties miniDubboProperties) {
        return new JdkDynamicProxy(clazz, interfaceConfig, miniDubboProperties).proxy;
    }

    /**
     * TODO 特殊方法不拦截。。
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("toString".equals(method.getName())) {
            return this.toString();
        }
        // todo group，attachment
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setInterfaceName(clazzName);
        rpcInvocation.setMethod(method);
        rpcInvocation.setParameterType(method.getParameterTypes());
        rpcInvocation.setArgs(args);
        rpcInvocation.setMethodName(method.getName());
        rpcInvocation.setVersion(interfaceConfig.getVersion());
        Class returnType = method.getReturnType();

        // todo 如果本地与实现类，且版本、group均匹配的话，直接调用本地的
        log.info("jdk调用：{}，代理类为：{}，返回类型：{}", rpcInvocation, proxy, returnType);
        // todo 通过接口配置决定用哪种策略
        Response response = (Response)failfastClusterInvoker.invoke(rpcInvocation);
        if (returnType == Void.class) {
            return null;
        }
        return response.getResult();
    }
}

