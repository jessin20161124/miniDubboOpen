package com.jessin.practice.dubbo.netty;

/**
 * 序列化接口，支持将一个对象序列化为字节流，也支持反序列化，有些方式不支持跨语言，例如jdk序列化
 * @Author: jessin
 * @Date: 2021/12/30 9:31 下午
 */
public interface Serializer {
    byte[] serialize(Object obj);

    <T> T deserialize(byte[] obj, Class<T> target);

}
