// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import java.lang.reflect.Field;

/**
 * 反射工具类
 *
 * @author luodong.seu
 */
public class ReflectUtils {

    /**
     * 获取类
     *
     * @param name 名称
     * @return Class
     */
    public static Class<?> getClassByName(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ignored) {
        }
        return null;
    }

    /**
     * 是否为指定的类的实例
     *
     * @param object 对象
     * @param args 类名列表
     * @return true: 是
     */
    public static boolean isInstance(Object object, String... args) {
        if (args == null || args.length == 0) {
            return false;
        }
        for (String arg : args) {
            Class<?> clazz = getClassByName(arg);
            if (null != clazz && clazz.isInstance(object)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取当前支持的Class类
     *
     * @param classNames Class名列表
     * @return 支持的Class
     */
    public static Class<?> getCurrentClass(String... classNames) {
        if (null == classNames || classNames.length == 0) {
            return null;
        }
        for (String s : classNames) {
            Class<?> clazz = getClassByName(s);
            if (null != clazz) {
                return clazz;
            }
        }
        return null;
    }

    /**
     * 反射获取某个属性的值
     *
     * @param object 对象
     * @param fieldName 字段名
     * @return Object
     */
    public static Object getFieldValue(Object object, String fieldName) {
        if (null == object) {
            return null;
        }

        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        } catch (Throwable e) {
            TLog.e(e);
        }
        return null;
    }
}
