package com.jessin.practice.dubbo.registry;

import org.apache.curator.framework.recipes.cache.TreeCacheEvent;

/**
 * @Author: jessin
 * @Date: 19-11-26 下午10:08
 */
public interface DataListener {

    void dataChanged(String path, Object value, TreeCacheEvent.Type eventType);
}
