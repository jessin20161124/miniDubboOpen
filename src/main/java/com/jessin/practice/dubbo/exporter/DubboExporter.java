package com.jessin.practice.dubbo.exporter;

import com.jessin.practice.dubbo.invoker.RpcInvocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务暴露
 * @Author: jessin
 * @Date: 19-11-27 下午8:13
 */
public class DubboExporter {
    private static Map<String, Object> exportServiceMap = new ConcurrentHashMap();

    public static void exportService(String clazzName, Object ref) {
        exportServiceMap.put(clazzName, ref);
    }

    public static Object getService(RpcInvocation rpcInvocation) {
        return exportServiceMap.get(rpcInvocation.getInterfaceName());
    }

}
