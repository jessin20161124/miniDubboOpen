package com.jessin.practice.dubbo.invoker;

import java.lang.reflect.Method;
import lombok.Data;

/**
 * TODO 是否需要序列化
 * @Author: jessin
 * @Date: 19-11-25 下午10:11
 */
@Data
public class RpcInvocation {

    private String version;

    private String interfaceName;

    private String methodName;

    /**
     * 不序列化，内部使用
     */
    private transient Method method;
    /**
     * 方法参数类型
     */
    private Class[] parameterType;

    private Object[] args;
    // todo attachment

}
