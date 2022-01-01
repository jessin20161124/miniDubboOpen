package com.jessin.practice.dubbo.exporter;

import com.jessin.practice.dubbo.config.InterfaceConfig;
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

    public static void exportService(String clazzName, InterfaceConfig interfaceConfig, Object ref) {
        String key = buildKey(clazzName, interfaceConfig.getVersion());
        exportServiceMap.put(key, ref);
    }

    public static Object getService(RpcInvocation rpcInvocation) {
        String key = buildKey(rpcInvocation.getInterfaceName(), rpcInvocation.getVersion());
        return exportServiceMap.get(key);
    }

    private static String buildKey(String clazzName, String version) {
        return String.format("%s_%s", clazzName, version);
    }

}
