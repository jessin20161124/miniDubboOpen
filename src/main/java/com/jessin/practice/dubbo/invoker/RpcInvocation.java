package com.jessin.practice.dubbo.invoker;

import java.lang.reflect.Method;
import lombok.Data;

@Data
public class RpcInvocation {

    /**
     * 服务级版本，而不是方法版本
     */
    private String version;

    private String interfaceName;

    private String methodName;

    /**
     * 不序列化，consumer内部使用
     */
    private transient Method method;
    /**
     * 方法参数类型
     */
    private Class[] parameterType;

    private Object[] args;
    // todo attachment

}
