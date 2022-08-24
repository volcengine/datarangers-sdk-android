// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.tracker;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.webkit.JavascriptInterface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.applog.AppLog;
import com.bytedance.applog.AppLogHelper;
import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.util.JavaScriptUtils;
import com.bytedance.applog.util.TLog;
import com.bytedance.applog.util.WebViewJsUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * JsBridge实现
 *
 * @author chenguanzhong
 * @date 2021/1/20
 */
public class AppLogBridge {

    /**
     * 初始化AppLogBridge代码，主要为了兼容js sdk 5.0以下版本调用AppLogBridge.hasStarted
     *
     * <p>annotation: 在js sdk v5.0+ 已经不需要AppLogBridge.hasStarted函数 TODO: 未来可以考虑去掉该兼容代码
     */
    public static final String compatAppLogBridgeCode =
            "if(typeof AppLogBridge !== 'undefined' && !AppLogBridge.hasStarted) { "
                    + "AppLogBridge.hasStarted = function(callback = undefined) {"
                    + "    if(callback) callback(AppLogBridge"
                    + ".hasStartedForJsSdkUnderV5_deprecated());\n"
                    + "    return AppLogBridge.hasStartedForJsSdkUnderV5_deprecated();"
                    + "};\n"
                    + "}";

    public static final class _AppLogBridge {
        /** appId为空则转发信息给所有实例 */
        private String appId;

        public _AppLogBridge() {}

        @JavascriptInterface
        public String getAppId() {
            return TextUtils.isEmpty(appId) ? AppLog.getAppId() : appId;
        }

        @Deprecated
        @JavascriptInterface
        public int hasStartedForJsSdkUnderV5_deprecated() {
            TLog.d("_AppLogBridge:hasStarted");
            return AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).hasStarted() ? 1 : 0;
        }

        @JavascriptInterface
        public String getSsid() {
            TLog.d("_AppLogBridge:getSsid");
            return AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).getSsid();
        }

        @JavascriptInterface
        public String getIid() {
            TLog.d("_AppLogBridge:getIid");
            return AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).getIid();
        }

        @JavascriptInterface
        public String getUuid() {
            TLog.d("_AppLogBridge:getUuid");
            return AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).getUserUniqueID();
        }

        @JavascriptInterface
        public String getUdid() {
            TLog.d("_AppLogBridge:getUdid");
            return AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).getUdid();
        }

        @JavascriptInterface
        public String getClientUdid() {
            TLog.d("_AppLogBridge:getClientUdid");
            return AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).getClientUdid();
        }

        @JavascriptInterface
        public String getOpenUdid() {
            TLog.d("_AppLogBridge:getOpenUdid");
            return AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).getOpenUdid();
        }

        @JavascriptInterface
        public String getAbSdkVersion() {
            TLog.d("_AppLogBridge:getAbSdkVersion");
            return AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).getAbSdkVersion();
        }

        @JavascriptInterface
        public String getABTestConfigValueForKey(String key, String defaultValue) {
            TLog.d("_AppLogBridge:getABTestConfigValueForKey: {}, {}", key, defaultValue);
            return AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId)
                    .getAbConfig(key, defaultValue);
        }

        @JavascriptInterface
        public void setUserUniqueId(@NonNull final String userUniqueId) {
            TLog.d("_AppLogBridge:setUuid: {}", userUniqueId);
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).setUserUniqueID(userUniqueId);
        }

        @JavascriptInterface
        public void profileSet(String jsonString) {
            TLog.d("_AppLogBridge:profileSet: {}", jsonString);
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId)
                    .profileSet(JavaScriptUtils.jsObjectStrToJson(jsonString));
        }

        @JavascriptInterface
        public void profileAppend(String jsonString) {
            TLog.d("_AppLogBridge:profileAppend: " + jsonString);
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId)
                    .profileAppend(JavaScriptUtils.jsObjectStrToJson(jsonString));
        }

        @JavascriptInterface
        public void profileIncrement(String jsonString) {
            TLog.d("_AppLogBridge:profileIncrement: " + jsonString);
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId)
                    .profileIncrement(JavaScriptUtils.jsObjectStrToJson(jsonString));
        }

        @JavascriptInterface
        public void profileSetOnce(String jsonString) {
            TLog.d("_AppLogBridge:profileSetOnce: {}", jsonString);
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId)
                    .profileSetOnce(JavaScriptUtils.jsObjectStrToJson(jsonString));
        }

        @JavascriptInterface
        public void profileUnset(String key) {
            TLog.d("_AppLogBridge:profileUnset: {}", key);
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).profileUnset(key);
        }

        @JavascriptInterface
        public void setHeaderInfo(String jsonString) {
            TLog.d("_AppLogBridge:setHeaderInfo: {}", jsonString);
            // 空参数，则原生调用传入null
            if (TextUtils.isEmpty(jsonString) || jsonString.equals("undefined")) {
                AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).setHeaderInfo(null);
                return;
            }
            JSONObject jsonObject = JavaScriptUtils.jsObjectStrToJson(jsonString);
            if (jsonObject == null) {
                return;
            }
            HashMap<String, Object> map = new HashMap<>();
            Iterator<String> iterator = jsonObject.keys();
            try {
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    map.put(key, jsonObject.get(key));
                }
            } catch (JSONException e) {
                TLog.e("_AppLogBridge: wrong Json format", e);
                return;
            }
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).setHeaderInfo(map);
        }

        @JavascriptInterface
        public void addHeaderInfo(String key, String value) {
            TLog.d("_AppLogBridge:addHeaderInfo: {}, {}", key, value);
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).setHeaderInfo(key, value);
        }

        @JavascriptInterface
        public void removeHeaderInfo(String key) {
            TLog.d("_AppLogBridge:removeHeaderInfo: {}", key);
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).removeHeaderInfo(key);
        }

        @JavascriptInterface
        public void onEventV3(@NonNull final String event, @Nullable final String params) {
            TLog.d("_AppLogBridge:onEventV3: {}, {}", event, params);
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId)
                    .onEventV3(event, JavaScriptUtils.jsObjectStrToJson(params));
        }

        /** 多实例下设置主实例AppId */
        @JavascriptInterface
        public void setNativeAppId(String appId) {
            TLog.d("_AppLogBridge:setNativeAppId: {}", appId);
            this.appId = (TextUtils.isEmpty(appId) || appId.equals("undefined")) ? null : appId;
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    public static void injectH5Bridge(View webView, String url) {
        if (checkAllowList(url)) {
            TLog.d("_AppLogBridge:injectH5Bridge to url:{}", url);
            JavaScriptUtils.addJavascriptInterface(webView, new _AppLogBridge(), "AppLogBridge");
        } else {
            TLog.d("_AppLogBridge:url:{} not pass allowlist", url);
        }
    }

    /**
     * 兼容旧的js sdk
     *
     * <p>适配 js sdk < 5.0 场景，5.0以后不需要执行该函数 TODO: 未来可以考虑去掉该兼容函数
     */
    public static void compatAppLogBridge(View view, String url) {
        WebViewJsUtil.evaluateJavascript(view, compatAppLogBridgeCode);
    }

    /**
     * 查找白名单
     *
     * @param url url
     * @return 是否在白名单内
     */
    private static boolean checkAllowList(String url) {
        if (TextUtils.isEmpty(url)) {
            TLog.d("Url is empty.");
            return false;
        }

        // 开启h5 bridge的实例
        List<AppLogInstance> enabledInstances =
                AppLogHelper.filterInstances(AppLogHelper.isH5BridgeEnabledMatcher);
        if (enabledInstances.isEmpty()) {
            TLog.d("No AppLog instance enabled h5 bridge.");
            return false;
        }

        // 读取白名单规则
        boolean allowAll = false;
        List<String> allowList = new ArrayList<>();
        for (AppLogInstance instance : enabledInstances) {
            if (null != instance.getInitConfig()) {
                if (instance.getInitConfig().isH5BridgeAllowAll()) {
                    allowAll = true;
                }
                if (null != instance.getInitConfig().getH5BridgeAllowlist()) {
                    allowList.addAll(instance.getInitConfig().getH5BridgeAllowlist());
                }
            }
        }

        // allow all
        if (allowAll) {
            return true;
        }

        // 白名单列表匹配
        if (allowList.isEmpty()) {
            return false;
        }

        String host;
        try {
            host = Uri.parse(url).getHost();
        } catch (Throwable e) {
            TLog.e("Parse host failed. url:" + url, e);
            return false;
        }

        if (TextUtils.isEmpty(host)) {
            // 无host的代码检查直接
            TLog.d("Host in url:{} is empty.", host, url);
            return false;
        }

        // 只要有一个实例满足白名单匹配即可
        for (String w : allowList) {
            try {
                String pattern = w.replaceAll("\\.", "\\\\.").replaceAll("\\*", "[^\\\\.]*");
                Pattern p = Pattern.compile(pattern);
                if (p.matcher(host).matches()) {
                    return true;
                }
            } catch (Throwable e) {
                TLog.e(e);
            }
        }
        return false;
    }
}
