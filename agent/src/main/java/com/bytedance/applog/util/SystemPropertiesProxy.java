// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.annotation.SuppressLint;

import java.lang.reflect.Method;

/**
 * Created by qianhong on 2017/5/8.
 */
public class SystemPropertiesProxy {
    private static volatile Object sSystemProperties;

    public SystemPropertiesProxy() {
    }

    private Object getSystemProperties() {
        if (sSystemProperties == null) {
            synchronized (SystemPropertiesProxy.class) {
                if (sSystemProperties == null) {
                    try {
                        Class<?> SystemProperties = Class.forName("android.os.SystemProperties");
                        sSystemProperties = SystemProperties.newInstance();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return sSystemProperties;
    }

    /**
     * 根据给定Key获取值.
     *
     * @return 如果不存在该key则返回空字符串
     * @throws IllegalArgumentException 如果key超过32个字符则抛出该异常
     */
    @SuppressLint("PrivateApi")
    public String get(String key) throws IllegalArgumentException {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getDeclaredMethod("get", String.class);
            return (String) method.invoke(clazz, key);
        } catch (Throwable e) {
            TLog.e(e);
        }
        try {
            Object systemProperties = getSystemProperties();
            Method get = systemProperties.getClass().getMethod("get", String.class);
            return (String) get.invoke(systemProperties, key);
        } catch (Throwable e) {
            TLog.e(e);
        }
        return "";
    }

}
