// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SystemPropertiesWithCache {
    private static final SystemPropertiesProxy sGetSystemProperties = new SystemPropertiesProxy();
    private static final Map<String, String> sCache = new ConcurrentHashMap<>();

    /**
     * 根据给定Key获取值.
     *
     * @return 如果不存在该key则返回空字符串
     * @throws IllegalArgumentException 如果key超过32个字符则抛出该异常
     */
    public static String get(String key) throws IllegalArgumentException {
        String value = sCache.get(key);
        if (value != null) {
            return value;
        }
        value = sGetSystemProperties.get(key);
        if (value != null) {
            sCache.put(key, value);
        }
        return value;
    }
}
