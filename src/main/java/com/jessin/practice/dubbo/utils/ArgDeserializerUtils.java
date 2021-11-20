package com.jessin.practice.dubbo.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: jessin
 * @Date: 2021/10/31 9:51 PM
 */
public class ArgDeserializerUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArgDeserializerUtils.class);

    /**
     * 参数对象在json反序列化时，得到的是JSONObject/JSONArray/int/string等对象，
     * 原始参数信息已经丢失，例如UserParam/List<UserParam>，需要根据class转换回原来的才能反射调用
     *
     * 这里支持第一层collection/map，大于一层不支持，
     * 例如不支持list<list<UserParam>>，map<string, map<string, userparam>
     * @param method 用于获取参数类型，带有泛型类型
     * @param argsType json序列后的参数类型，已经丢失泛型类型
     * @param args 参数值
     * @return
     */
    public static Object[] parseArgs(Method method, Class[] argsType, Object[] args) {
        if (argsType == null || args == null) {
            return new Object[0];
        }
        Object[] result = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof JSONObject) {
                if (Map.class.isAssignableFrom(argsType[i])) {
                    Type[] types = method.getGenericParameterTypes();
                    ParameterizedType pType = (ParameterizedType)types[i];
                    LOGGER.info("原来类型：{}", pType.getRawType());
                    Type[] actualTypeArguments = pType.getActualTypeArguments();
                    for(Type actualTypeArgument: actualTypeArguments) {
                        LOGGER.info("泛型类型：{}", actualTypeArgument);
                    }
                    Class keyType = (Class)actualTypeArguments[0];
                    Class valueType = (Class)actualTypeArguments[1];
                    Map oldMap = (Map)result[i];
                    Map newMap = createMap((Class)pType.getRawType(), oldMap.size());
                    oldMap.forEach((key, value) -> {
                        Object newKey = key;
                        if (key instanceof JSONObject) {
                            newKey = ((JSONObject) key).toJavaObject(keyType);
                        }
                        Object newValue = value;
                        if (value instanceof JSONObject) {
                            newValue = ((JSONObject) value).toJavaObject(valueType);
                        }
                        newMap.put(newKey, newValue);
                    });
                    result[i] = newMap;
                } else {
                    // 如果类型是hashMap，会报错，http://blog.kail.xyz/post/2019-06-02/other/json.toJavaObject.html
                    // result[i] = ((JSONObject) arg).toJavaObject(argsType[i]);
                    result[i] = JSON.parseObject(JSON.toJSONString(arg), argsType[i]);
                }
            } else if (arg instanceof JSONArray) {
                // 需要看类型是array/Collection
                if (argsType[i].isArray()) {
                    Class elementType = argsType[i].getComponentType();
                    List list = ((JSONArray) arg).toJavaList(elementType);
                    result[i] = Array.newInstance(elementType, list.size());
                } else if (Collection.class.isAssignableFrom(argsType[i])) {
                    // 泛型已经被擦除，这里通过方法来获取参数的泛型类型
                    Type[] types = method.getGenericParameterTypes();
                    ParameterizedType pType = (ParameterizedType)types[i];
                    LOGGER.info("原来类型：{}", pType.getRawType());
                    Type[] actualTypeArguments = pType.getActualTypeArguments();
                    for(Type actualTypeArgument: actualTypeArguments) {
                        LOGGER.info("泛型类型：{}", actualTypeArgument);
                    }
                    Class elementType = (Class)actualTypeArguments[0];
                    List list = ((JSONArray) arg).toJavaList(elementType);
                    Collection collection = createCollection((Class)pType.getRawType(), ((JSONArray)arg).size());
                    collection.addAll(list);
                    result[i] = collection;
                } else {
                    throw new RuntimeException("不可反序列化的数组类型：" + argsType[i].getName());
                }
            } else {
                // 原始类型
                result[i] = arg;
            }
        }
        return result;
    }

    protected static Collection<Object> createCollection(Class<? extends Collection> collectionType, int initialCapacity) {
        if (!collectionType.isInterface()) {
            try {
                return collectionType.newInstance();
            }
            catch (Exception ex) {
                throw new IllegalArgumentException(
                        "Could not instantiate collection class [" + collectionType.getName() + "]: " + ex.getMessage());
            }
        }
        else if (List.class == collectionType) {
            return new ArrayList<Object>(initialCapacity);
        }
        else if (SortedSet.class == collectionType) {
            return new TreeSet<Object>();
        }
        else {
            return new LinkedHashSet<Object>(initialCapacity);
        }
    }

    protected static Map<Object, Object> createMap(Class<? extends Map> mapType, int initialCapacity) {
        if (!mapType.isInterface()) {
            try {
                return mapType.newInstance();
            }
            catch (Exception ex) {
                throw new IllegalArgumentException(
                        "Could not instantiate map class [" + mapType.getName() + "]: " + ex.getMessage());
            }
        }
        else if (SortedMap.class == mapType) {
            return new TreeMap<Object, Object>();
        }
        else {
            return new LinkedHashMap<Object, Object>(initialCapacity);
        }
    }
}
