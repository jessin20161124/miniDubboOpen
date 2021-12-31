package com.jessin.practice.dubbo.netty;

/**
 * @Author: jessin
 * @Date: 2021/12/30 9:27 下午
 */
public interface Constants {
    byte REQUEST = 0;
    byte RESPONSE = 1;

    Serializer SERIALIZER = new FastjsonSerializer();
}
