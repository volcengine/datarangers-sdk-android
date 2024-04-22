// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import com.bytedance.applog.log.LoggerImpl;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/** Created by qianhong on 2017/5/8. */
public class SystemPropertiesProxy {
    private static volatile Object sSystemProperties;
    private final List<String> loggerTags = Collections.singletonList("SystemPropertiesProxy");

    public SystemPropertiesProxy() {}

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
    public String get(String key) throws IllegalArgumentException {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getDeclaredMethod("get", String.class);
            return (String) method.invoke(clazz, key);
        } catch (Throwable e) {
            LoggerImpl.global().error(loggerTags, "Get key:{} value failed", e, key);
        }
        try {
            Object systemProperties = getSystemProperties();
            Method get = systemProperties.getClass().getMethod("get", String.class);
            return (String) get.invoke(systemProperties, key);
        } catch (Throwable e) {
            LoggerImpl.global().error(loggerTags, "Get key:{} value by reflection failed", e, key);
        }
        return "";
    }

    public String get(String key, String def) throws IllegalArgumentException {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getDeclaredMethod("get", String.class);
            return (String) method.invoke(clazz, key);
        } catch (Throwable e) {
            LoggerImpl.global()
                    .error(loggerTags, "Get key:{} value default:{} failed", e, key, def);
        }
        try {
            Object systemProperties = getSystemProperties();
            Method get = systemProperties.getClass().getMethod("get", String.class, String.class);
            return (String) get.invoke(systemProperties, key, def);
        } catch (Throwable e) {
            LoggerImpl.global()
                    .error(
                            loggerTags,
                            "Get key:{} value default:{} by reflection failed",
                            e,
                            key,
                            def);
        }
        return def;
    }

    public Integer getInt(String key, int def) throws IllegalArgumentException {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getDeclaredMethod("getInt", String.class, Integer.class);
            return (Integer) method.invoke(clazz, key, def);
        } catch (Throwable e) {
            LoggerImpl.global()
                    .error(loggerTags, "Get int key:{} value default:{} failed", e, key, def);
        }
        try {
            Object systemProperties = getSystemProperties();
            Method getInt =
                    systemProperties.getClass().getMethod("getInt", String.class, int.class);
            return (Integer) getInt.invoke(systemProperties, key, def);
        } catch (Throwable e) {
            LoggerImpl.global()
                    .error(
                            loggerTags,
                            "Get int key:{} value default:{} by reflection failed",
                            e,
                            key,
                            def);
        }
        return def;
    }
}
