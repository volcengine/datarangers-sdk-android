// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import com.bytedance.applog.log.IAppLogLogger;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 校验器工具类
 *
 * @author luodong.seu
 */
public class Validator {
    private static final List<String> loggerTags = Collections.singletonList("Validator");

    /** 合法的事件名的正则 */
    private static final Pattern keyValidPattern =
            Pattern.compile("^[a-zA-Z0-9][a-z0-9A-Z_ .-]{1,255}$");

    /** 事件参数的白名单key */
    private static final List<String> whiteListKeys =
            Arrays.asList(
                    "$inactive",
                    "$inline",
                    "$target_uuid_list",
                    "$source_uuid",
                    "$is_spider",
                    "$source_id",
                    "$is_first_time");

    /** 校验事件 */
    public static void testEvent(IAppLogLogger logger, String name, JSONObject params) {
        testEventName(logger, name);
        testEventParams(logger, name, params);
    }

    /**
     * 校验名称
     *
     * @param name 事件名
     * @return true: pass
     */
    public static void testEventName(IAppLogLogger logger, String name) {
        if (Utils.isEmpty(name)) {
            logger.warn(loggerTags, "Event name must not be empty!");
            return;
        }
        if (!keyValidPattern.matcher(name).matches()) {
            logger.warn(loggerTags, "Event [" + name + "] name is invalid!");
        }
        if (name.startsWith("__")) {
            logger.warn(loggerTags, "Event [" + name + "] name should not start with __!");
        }
    }

    /**
     * 校验事件参数
     *
     * @param params 事件参数
     * @return true: pass
     */
    public static void testEventParams(IAppLogLogger logger, String name, JSONObject params) {
        if (null == params || params.length() == 0) {
            return;
        }
        Iterator<String> itr = params.keys();
        final String eventName = Utils.toString(name);
        while (itr.hasNext()) {
            String key = itr.next();
            if (Utils.isEmpty(key)) {
                logger.warn(loggerTags, "Event [" + eventName + "] param key must not be empty!");
            }
            if (!whiteListKeys.contains(key)) {
                if (!keyValidPattern.matcher(key).matches()) {
                    logger.warn(
                            loggerTags,
                            "Event [" + eventName + "] param key [" + key + "] is invalid!");
                }
                if (key.startsWith("__")) {
                    logger.warn(
                            loggerTags,
                            "Event ["
                                    + eventName
                                    + "] param key ["
                                    + key
                                    + "] should not start with __!");
                }
            }
            Object value = params.opt(key);
            if (value instanceof String && ((String) value).length() > 1024) {
                logger.warn(
                        loggerTags,
                        "Event ["
                                + eventName
                                + "] param key ["
                                + key
                                + "] value is limited to a maximum of 1024 characters!");
            }
        }
    }

    /**
     * 校验自定义的公共头
     *
     * @param headers Map
     * @return true: pass
     */
    public static void testCustomHeaders(IAppLogLogger logger, Map<String, Object> headers) {
        if (null == headers) {
            return;
        }
        for (String name : headers.keySet()) {
            if (Utils.isEmpty(name)) {
                logger.warn(loggerTags, "Header name must not be empty!");
            }
            if (!whiteListKeys.contains(name)) {
                if (!keyValidPattern.matcher(name).matches()) {
                    logger.warn(loggerTags, "Header [" + name + "] name is invalid!");
                }
                if (name.startsWith("__")) {
                    logger.warn(loggerTags, "Header [" + name + "] name should not start with __!");
                }
            }
            Object value = headers.get(name);
            if (value instanceof String && ((String) value).length() > 1024) {
                logger.warn(
                        loggerTags,
                        "Header [" + name + "] value is limited to a maximum of 1024 characters!");
            }
        }
    }

    /**
     * 校验Profile的参数
     *
     * @param params 参数
     * @return true: pass
     */
    public static void testProfileParams(IAppLogLogger logger, JSONObject params) {
        if (null == params || params.length() == 0) {
            return;
        }
        Iterator<String> itr = params.keys();
        while (itr.hasNext()) {
            String key = itr.next();
            if (Utils.isEmpty(key)) {
                logger.warn(loggerTags, "Profile key must not be empty!");
            }
            if (!keyValidPattern.matcher(key).matches()) {
                logger.warn(loggerTags, "Profile param [" + key + "] name is invalid!");
            }
            Object value = params.opt(key);
            if (value instanceof String && ((String) value).length() > 1024) {
                logger.warn(
                        loggerTags,
                        "Profile param ["
                                + key
                                + "] value is limited to a maximum of 1024 "
                                + "characters!");
            }
        }
    }
}
