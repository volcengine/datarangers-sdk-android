// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.view.View;

import com.bytedance.applog.log.LoggerImpl;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Collections;

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
    public static JSONObject jsObjectStrToJson(String jsonString) {
        if (Utils.isEmpty(jsonString)) {
            return null;
        }
        try {
            return new JSONObject(jsonString);
        } catch (Throwable e) {
            LoggerImpl.global()
                    .error(Collections.singletonList("JavaScriptUtils"), "JSON handle failed", e);
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
            LoggerImpl.global()
                    .error(
                            Collections.singletonList("JavaScriptUtils"),
                            "addJavascriptInterface failed",
                            e);
        }
    }
}
