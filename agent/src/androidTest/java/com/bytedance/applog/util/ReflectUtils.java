// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 反射工具类
 * Created by luoqiaoyou on 2020-01-06.
 */

public class ReflectUtils {

    public static <T> T getFieldValue(Object object, String name) {
        try {
            Class clss = object instanceof Class ? (Class) object : object.getClass();
            Field field = clss.getDeclaredField(name);
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            return (T) field.get(object);
        } catch (NoSuchFieldException ignore) {

        } catch (IllegalAccessException ignore) {

        }
        return null;
    }

    public static <T> void setFieldValue(Object object, String name, T value) {
        try {
            Class clss = object instanceof Class ? (Class) object : object.getClass();
            Field field = clss.getDeclaredField(name);
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            field.set(object, value);
        } catch (NoSuchFieldException ignore) {

        } catch (IllegalAccessException ignore) {

        }
    }

    public static void execStaticMethod(Object object, String name) {
        try {
            Class clss = object instanceof Class ? (Class) object : object.getClass();
            Method field = clss.getDeclaredMethod(name, (Class<?>[]) null);
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            field.invoke(null);
        } catch (IllegalAccessException ignore) {
        } catch (NoSuchMethodException ignore) {
        } catch (InvocationTargetException ignore) {
        }
    }
}
