package com.jessin.practice.dubbo;

import static org.junit.Assert.assertTrue;

import com.jessin.practice.dubbo.netty.NettyServer;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        // 开启netty server
        NettyServer nettyServer = new NettyServer(20880);

    }
}
