package com.jessin.practice.dubbo.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @Author: jessin
 * @Date: 2022/1/8 6:46 下午
 */
public class StringUtils {
    public static String toString(Throwable e) {
        StringWriter w = new StringWriter();
        PrintWriter p = new PrintWriter(w);
        p.print(e.getClass().getName());
        if (e.getMessage() != null) {
            p.print(": " + e.getMessage());
        }
        p.println();
        try {
            e.printStackTrace(p);
            return w.toString();
        } finally {
            p.close();
        }
    }
}
