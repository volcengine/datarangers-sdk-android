// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.text.TextUtils;

import com.bytedance.applog.log.LoggerImpl;

/**
 * 检查工具类
 *
 * @author luodong.seu
 */
public class Assert {

    /**
     * 要求value为true
     *
     * @param value 检查的值
     * @param message 错误提示
     * @return 是否断言
     */
    public static boolean t(boolean value, String message) {
        if (value) {
            return false;
        }
        e(message);
        return true;
    }

    /**
     * 要求value为false
     *
     * @param value 检查的值
     * @param message 错误提示
     * @return 是否断言
     */
    public static boolean f(boolean value, String message) {
        return t(!value, message);
    }

    /**
     * 要求value是空字符串
     *
     * @param value 检查的值
     * @param message 错误提示
     * @return 是否断言
     */
    public static boolean empty(String value, String message) {
        return t(TextUtils.isEmpty(value), message);
    }

    /**
     * 要求value是非空字符串
     *
     * @param value 检查的值
     * @param message 错误提示
     * @return 是否断言
     */
    public static boolean notEmpty(String value, String message) {
        return f(TextUtils.isEmpty(value), message);
    }

    /**
     * 要求value是非null
     *
     * @param value 检查的值
     * @param message 错误提示
     * @return 是否断言
     */
    public static boolean notNull(Object value, String message) {
        return f(null == value, message);
    }

    /**
     * 要求value是null
     *
     * @param value 检查的值
     * @param message 错误提示
     * @return 是否断言
     */
    public static boolean isNull(Object value, String message) {
        return t(null == value, message);
    }

    /**
     * 抛出异常或打印日志
     *
     * @param message 错误提示
     */
    public static void e(String message) {
        LoggerImpl.global().ast("[Assert failed] {}", null, message);
    }
}
