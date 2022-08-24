// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.support.annotation.Nullable;
import android.view.View;

import org.json.JSONObject;

import java.lang.reflect.Method;

/**
 * @author chenguanzhong
 * @date 2021/1/21
 */
public class JavaScriptUtils {

    /**
     * 将jsObject的Json字符串转为Json js可能传入undefined
     *
     * @param jsonString json字符串
     * @return JSONObject 出错为null
     */
    @Nullable
    public static JSONObject jsObjectStrToJson(@Nullable String jsonString) {
        try {
            return new JSONObject(jsonString);
        } catch (Throwable ignored) {
            TLog.e("wrong Json format, return null pointer", null);
            return null;
        }
    }

    /**
     * 添加JS接口
     *
     * @param view WebView对象
     * @param bridge 接口
     * @param name 名称
     */
    @SuppressWarnings("SameParameterValue")
    public static void addJavascriptInterface(View view, Object bridge, String name) {
        if (!ClassHelper.isWebView(view)) {
            return;
        }
        try {
            Class<?> clazz = view.getClass();
            Method method = clazz.getMethod("addJavascriptInterface", Object.class, String.class);
            method.invoke(view, bridge, name);
        } catch (Throwable e) {
            TLog.e(e);
        }
    }
}
