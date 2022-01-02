package com.jessin.practice.dubbo.netty;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.jessin.practice.dubbo.exporter.DubboExporter;
import com.jessin.practice.dubbo.invoker.RpcInvocation;
import com.jessin.practice.dubbo.transport.DefaultFuture;
import com.jessin.practice.dubbo.transport.Request;
import com.jessin.practice.dubbo.transport.Response;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;

/**
 * todo protobuf，序列化需要实现跨语言，例如jdk序列化不支持跨语言
 * https://blog.csdn.net/fly910905/article/details/81504388
 * 对于调用服务端异常时，会返回异常栈，需要开启自动化类型支持，否则反序列化会报错，需要注意
 * @Author: jessin
 * @Date: 2021/12/30 9:33 下午
 */
@Slf4j
public class FastjsonSerializer implements Serializer {

    /**
     * 这种方式可能有安全问题...
     */
    private static ParserConfig parserConfig = new ParserConfig();
    static {
        parserConfig.setAutoTypeSupport(true);
    }
    private static final ConcurrentMap<String, Method> NAME_METHODS_CACHE = new ConcurrentHashMap<String, Method>();
    private static final ConcurrentMap<Class<?>, ConcurrentMap<String, Field>> CLASS_FIELD_CACHE = new ConcurrentHashMap<Class<?>, ConcurrentMap<String, Field>>();

    @Override
    public byte[] serialize(Object msg) {
        try {
            // 序列化时，把类信息带上，保证泛化信息也带上，正常的话需要带上的
            // 例如Map<String, UserParam>，则value也会带上UserParam信息，可以反序列化成功
            String jsonStr = JSON.toJSONString(msg, SerializerFeature.WriteClassName);
//            String jsonStr = JSON.toJSONString(msg);
            byte[] wordBytes = jsonStr.getBytes(StandardCharsets.UTF_8.name());
            return wordBytes;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("serial error", e);
        }
    }

    /**
     * 由于fastjson在开启autoType功能时，仍然有部分未带@type 类型信息的情况，例如List<User>，这里需要依据本地方法进行反序列化
     *   可以利用接收方方法信息反射重建，通过本地方法的返回类型进行反序列化
     *    Response.result字段为JSONObject。
     *    返回list/map/int,A<V>，返回值也直接得到了信息，不需要反序列化了
     *
     *
     * @param realData
     * @param target
     * @param <T>
     * @return
     */
    @Override
    public <T> T deserialize(byte[] realData, Class<T> target) {
        try {
            String realStr = new String(realData, StandardCharsets.UTF_8.name());
            // 反序列化时，支持识别传递的类，保证有泛型信息时也能成功，例如Map<String, UserParam>，则value的类型是UserParam，而不是JSONObject
            T data = JSON.parseObject(realStr, target, parserConfig);
            if (target == Request.class) {
                Request request = ((Request)data);
                if (!request.isEvent()) {
                    RpcInvocation rpcInvocation = request.getRpcInvocation();
                    Object obj = DubboExporter.getService(rpcInvocation);
                    if (obj != null) {
                        Method method = obj.getClass().getMethod(rpcInvocation.getMethodName(), rpcInvocation.getParameterType());
                        Object[] newArgs = convert(rpcInvocation.getArgs(), method.getParameterTypes(), method.getGenericParameterTypes());
                        rpcInvocation.setArgs(newArgs);
                    }
                }
            } else if (target == Response.class) {
                Response response = ((Response)data);
                if (!response.isEvent()) {
                    DefaultFuture.getRequest(response.getId()).ifPresent(
                            request -> {
                                Method method = request.getRpcInvocation().getMethod();
                                // todo CompletableFuture这里跟序列化无关，需要提到外层
                                // 对于返回future类型，服务端返回的是具体的值，这里客户端需要特殊处理下
                                if (CompletableFuture.class.isAssignableFrom(method.getReturnType())) {
                                    Type genericType = method.getGenericReturnType();
                                    Type valueType = getGenericClassByIndex(genericType, 0);
                                    // CompletableFuture<ValueType>
                                    Object newResult;
                                    if (valueType != null) {
                                        newResult = convert(response.getResult(), null, valueType);
                                    } else {
                                        // 没有泛型信息，不做处理，依赖@type实现反序列化正确
                                        newResult = response.getResult();
                                    }
                                    response.setResult(newResult);
                                } else {
                                    Object newResult = convert(response.getResult(), method.getReturnType(), method.getGenericReturnType());
                                    response.setResult(newResult);
                                }
                            }
                    );
                }
            }
            return data;
        } catch (Exception e) {
            throw new RuntimeException("deserialize error", e);
        }
    }

    /**
     * 参数对象在json反序列化时，得到的是JSONObject/JSONArray/int/string等对象，
     * 原始参数信息已经丢失，例如UserParam/List<UserParam>，需要根据class转换回原来的才能反射调用
     *
     * 这里支持第一层collection/map，大于一层不支持，
     * 例如不支持list<list<UserParam>>，map<string, map<string, userparam>
     * @param args 参数值
     * @param clazzTypes 方法上的参数类型，原来json序列后的参数类型已经丢失泛型类型
     * @param genericTypes  用于获取参数类型，带有泛型类型
     * @return
     */
    public Object[] convert(Object[] args, Class<?>[] clazzTypes, Type[] genericTypes) {
        if (args == null || args.length == 0) {
            return args;
        }
        Object[] result = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = convert(args[i], clazzTypes[i], genericTypes[i]);
        }
        return result;
    }

    /**
     * 常见类型可以序列化，其他的自定义序列化器
     * @param arg
     * @param clazzType
     * @param genericType
     * @return
     */
    public Object convert(Object arg, Class clazzType, Type genericType) {
        if (arg == null || clazzType == Void.class) {
            return null;
        }
        if (clazzType == null) {
            if (genericType instanceof Class) {
                clazzType = (Class)genericType;
            } else if (genericType instanceof ParameterizedType) {
                // todo 一定是class?
                clazzType = (Class)((ParameterizedType)genericType).getRawType();
            } else {
                clazzType = arg.getClass();
            }
        }
        // 通过上面第一次转换后，如果还是JSON对象，则需要进一步转化，如果确实传递的是JSONObject，存在重复操作
        if (arg instanceof JSONObject) {
            if (Map.class.isAssignableFrom(clazzType)) {
                Type keyType = getGenericClassByIndex(genericType, 0);
                Type valueType = getGenericClassByIndex(genericType, 1);
                Map oldMap = (Map)arg;
                Map newMap = createMap(clazzType, oldMap.size());
                oldMap.forEach((key, value) -> {
                    Object newKey = convert(key, null, keyType);
                    Object newValue = convert(value, null, valueType);
                    newMap.put(newKey, newValue);
                });
                return newMap;
            } else if (clazzType.isInterface()) {
                // todo 如果是接口，泛化调用的话，使用的是jdk代理。这里暂时使用autotype
                // 如果类型是hashMap，会报错，http://blog.kail.xyz/post/2019-06-02/other/json.toJavaObject.html
                // result[i] = ((JSONObject) arg).toJavaObject(argsType[i]);
                // beanUtils.copy???MapStruct
                // 复杂对象也可能带有泛型信息
//                Object obj = JSON.parseObject(JSON.toJSONString(arg), clazzType);
                return arg;
            } else {

                Object dest;
                try {
                     dest = clazzType.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("复杂对象反序列化失败", e);
                }
                Map oldMap = (Map)arg;
                oldMap.forEach((key, value) -> {
                    if (value == null || !(key instanceof String)) {
                        return;
                    }
                    Method setterMethod = getSetterMethod(dest.getClass(), (String)key, value.getClass());
                    if (setterMethod != null) {
                        if (!setterMethod.isAccessible()) {
                            setterMethod.setAccessible(true);
                        }
                        Object newValue = convert(value, setterMethod.getParameterTypes()[0], setterMethod.getGenericParameterTypes()[0]);
                        try {
                            setterMethod.invoke(dest, newValue);
                        } catch (Exception e) {
                            String exceptionDescription = "Failed to set pojo " + dest.getClass().getSimpleName() + " property " + key
                                    + " value " + value + "(" + value.getClass() + "), cause: " + e.getMessage();
                            log.error(exceptionDescription, e);
                            throw new RuntimeException(exceptionDescription, e);
                        }
                    } else {
                        Field field = getField(dest.getClass(), (String)key);
                        if (field != null) {
                            Object newValue = convert(value, field.getType(), field.getGenericType());
                            field.setAccessible(true);
                            try {
                                field.set(dest, newValue);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException("Failed to set field " + key + " of pojo " + dest.getClass().getName() + " : " + e.getMessage(), e);
                            }
                        }
                    }
                });
                return dest;
            }
        } else if (arg instanceof JSONArray) {
            // 需要看类型是array/Collection
            if (clazzType.isArray()) {
                Class elementType = clazzType.getComponentType();
                int len = ((JSONArray)arg).size();
                // todo 数组一定没有泛型类型吗？genericType
                Object dest = Array.newInstance(elementType, len);
                for (int i = 0; i < len; i++) {
                    Object newResult = convert(((JSONArray)arg).get(i), elementType, null);
                    Array.set(dest, i, newResult);
                }
                return dest;
            } else if (Collection.class.isAssignableFrom(clazzType)) {
                Type elementType = getGenericClassByIndex(genericType, 0);
                Collection collection = createCollection(clazzType, ((JSONArray)arg).size());
                ((JSONArray)arg).forEach(element -> {
                    Object newResult = convert(element, null, elementType);
                    collection.add(newResult);
                });
                return collection;
            } else {
                throw new RuntimeException("不可反序列化的数组类型：" + clazzType.getName());
            }
        } else if (arg instanceof String && clazzType.isEnum()) {
            return Enum.valueOf((Class<Enum>) clazzType, (String) arg);
        } else {
            // 原始类型
            return arg;
        }
    }

    private static Method getSetterMethod(Class<?> cls, String property, Class<?> valueCls) {
        String name = "set" + property.substring(0, 1).toUpperCase() + property.substring(1);
        Method method = NAME_METHODS_CACHE.get(cls.getName() + "." + name + "(" + valueCls.getName() + ")");
        if (method == null) {
            try {
                method = cls.getMethod(name, valueCls);
            } catch (NoSuchMethodException e) {
                for (Method m : cls.getMethods()) {
                    if (isBeanPropertyWriteMethod(m) && m.getName().equals(name)) {
                        method = m;
                    }
                }
            }
            if (method != null) {
                NAME_METHODS_CACHE.put(cls.getName() + "." + name + "(" + valueCls.getName() + ")", method);
            }
        }
        return method;
    }

    public static boolean isBeanPropertyWriteMethod(Method method) {
        return method != null
                && Modifier.isPublic(method.getModifiers())
                && !Modifier.isStatic(method.getModifiers())
                && method.getDeclaringClass() != Object.class
                && method.getParameterTypes().length == 1
                && method.getName().startsWith("set")
                && method.getName().length() > 3;
    }

    private static Field getField(Class<?> cls, String fieldName) {
        Field result = null;
        if (CLASS_FIELD_CACHE.containsKey(cls) && CLASS_FIELD_CACHE.get(cls).containsKey(fieldName)) {
            return CLASS_FIELD_CACHE.get(cls).get(fieldName);
        }
        try {
            result = cls.getDeclaredField(fieldName);
            result.setAccessible(true);
        } catch (NoSuchFieldException e) {
            for (Field field : cls.getFields()) {
                if (fieldName.equals(field.getName()) && isPublicInstanceField(field)) {
                    result = field;
                    break;
                }
            }
        }
        if (result != null) {
            ConcurrentMap<String, Field> fields = CLASS_FIELD_CACHE.get(cls);
            if (fields == null) {
                fields = new ConcurrentHashMap<String, Field>();
                CLASS_FIELD_CACHE.putIfAbsent(cls, fields);
            }
            fields = CLASS_FIELD_CACHE.get(cls);
            fields.putIfAbsent(fieldName, result);
        }
        return result;
    }

    private static Type getGenericClassByIndex(Type genericType, int index) {
        // 泛型已经被擦除，这里通过方法来获取参数的泛型类型
        Type clazz = null;
        // find parameterized type
        if (genericType instanceof ParameterizedType) {
            ParameterizedType t = (ParameterizedType) genericType;
            // todo 这个一定是class??
            log.info("原来类型：{}", t.getRawType());
            Type[] types = t.getActualTypeArguments();
            for(Type actualTypeArgument: types) {
                log.info("泛型类型：{}", actualTypeArgument);
            }
            clazz = types[index];
        }
        return clazz;
    }

    public static boolean isPublicInstanceField(Field field) {
        return Modifier.isPublic(field.getModifiers())
                && !Modifier.isStatic(field.getModifiers())
                && !Modifier.isFinal(field.getModifiers())
                && !field.isSynthetic();
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
