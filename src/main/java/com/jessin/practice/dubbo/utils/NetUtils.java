package com.jessin.practice.dubbo.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * @Author: jessin
 * @Date: 19-11-27 下午11:09
 */

public class NetUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetUtils.class);

    public static void main(String[] args) {
        System.out.println(getServerIp());
    }
    /**
     * 获取服务器地址
     *
     * @return Ip地址
     */
    public static String getServerIp() {
        String publicIp = System.getProperty("public.ip");
        if (publicIp != null && publicIp.length() > 0) {
            return publicIp;
        }
        Enumeration<NetworkInterface> allNetInterfaces;
        try {
            allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = allNetInterfaces.nextElement();

                Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    ip = addresses.nextElement();
                    LOGGER.info("ip :{}", ip);
                    if (!ip.isSiteLocalAddress() && !ip.isLoopbackAddress() && !ip.getHostAddress().contains(":") && ip instanceof Inet4Address) {
                        return ip.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            LOGGER.error("get server ip error", e);
        }

        InetAddress jdkSuppliedAddress;
        try {
            jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress != null) {
                return jdkSuppliedAddress.getHostAddress();
            }
        } catch (UnknownHostException e) {
            LOGGER.error("get fallback server ip error", e);
        }
        throw new RuntimeException("unknown local ip");
    }
}
