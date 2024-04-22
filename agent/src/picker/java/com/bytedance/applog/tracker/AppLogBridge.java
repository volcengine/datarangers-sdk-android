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
import com.bytedance.applog.log.AbstractAppLogLogger;
import com.bytedance.applog.log.IAppLogLogger;
import com.bytedance.applog.log.LoggerImpl;
import com.bytedance.applog.util.JavaScriptUtils;
import com.bytedance.applog.util.WebViewJsUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
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
    private static final List<String> loggerTags = Collections.singletonList("AppLogBridge");

    /**
     * 初始化AppLogBridge代码，主要为了兼容js sdk 5.0以下版本调用AppLogBridge.hasStarted
     *
     * <p>annotation: 在js sdk v5.0+ 已经不需要AppLogBridge.hasStarted函数 TODO: 未来可以考虑去掉该兼容代码
     */
    public static final String compatAppLogBridgeCode =
            "if(typeof AppLogBridge !== 'undefined' && !AppLogBridge.hasOwnProperty('hasStarted')"
                    + ") { AppLogBridge.hasStarted = function(callback) {if(callback) "
                    + "callback(AppLogBridge.hasStartedForJsSdkUnderV5_deprecated());  return AppLogBridge.hasStartedForJsSdkUnderV5_deprecated();};}";

    public static final class _AppLogBridge {

        /**
         * appId为空则转发信息给所有实例
         */
        private String appId;

        public _AppLogBridge() {
        }

        @JavascriptInterface
        public String getAppId() {
            return TextUtils.isEmpty(appId) ? AppLog.getAppId() : appId;
        }

        @Deprecated
        @JavascriptInterface
        public int hasStartedForJsSdkUnderV5_deprecated() {
            log("_AppLogBridge:hasStarted");
            return AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).hasStarted() ? 1 : 0;
        }

        @JavascriptInterface
        public String getSsid() {
            log("_AppLogBridge:getSsid");
            return AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).getSsid();
        }

        @JavascriptInterface
        public String getIid() {
            log("_AppLogBridge:getIid");
            return AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).getIid();
        }

        @JavascriptInterface
        public String getUuid() {
            log("_AppLogBridge:getUuid");
            return AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).getUserUniqueID();
        }

        @JavascriptInterface
        public String getUdid() {
            log("_AppLogBridge:getUdid");
            return AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).getUdid();
        }

        @JavascriptInterface
        public String getClientUdid() {
            log("_AppLogBridge:getClientUdid");
            return AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).getClientUdid();
        }

        @JavascriptInterface
        public String getOpenUdid() {
            log("_AppLogBridge:getOpenUdid");
            return AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).getOpenUdid();
        }

        @JavascriptInterface
        public String getAbSdkVersion() {
            log("_AppLogBridge:getAbSdkVersion");
            return AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).getAbSdkVersion();
        }

        @JavascriptInterface
        public String getABTestConfigValueForKey(String key, String defaultValue) {
            log("_AppLogBridge:getABTestConfigValueForKey: {}, {}", key, defaultValue);
            return AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId)
                    .getAbConfig(key, defaultValue);
        }

        @JavascriptInterface
        public String getAllAbTestConfigs() {
            log("_AppLogBridge:getAllAbTestConfigs");
            JSONObject allAbTestConfigs = AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId)
                    .getAllAbTestConfigs();
            return allAbTestConfigs != null ? allAbTestConfigs.toString() : null;
        }

        @JavascriptInterface
        public void setExternalAbVersion(String version) {
            log("_AppLogBridge:setExternalAbVersion: {}", version);
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).setExternalAbVersion(version);
        }

        @JavascriptInterface
        public void setUserUniqueId(@NonNull final String userUniqueId) {
            log("_AppLogBridge:setUuid: {}", userUniqueId);
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).setUserUniqueID(userUniqueId);
        }

        @JavascriptInterface
        public void profileSet(String jsonString) {
            log("_AppLogBridge:profileSet: {}", jsonString);
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId)
                    .profileSet(JavaScriptUtils.jsObjectStrToJson(jsonString));
        }

        @JavascriptInterface
        public void profileAppend(String jsonString) {
            log("_AppLogBridge:profileAppend: " + jsonString);
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId)
                    .profileAppend(JavaScriptUtils.jsObjectStrToJson(jsonString));
        }

        @JavascriptInterface
        public void profileIncrement(String jsonString) {
            log("_AppLogBridge:profileIncrement: " + jsonString);
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId)
                    .profileIncrement(JavaScriptUtils.jsObjectStrToJson(jsonString));
        }

        @JavascriptInterface
        public void profileSetOnce(String jsonString) {
            log("_AppLogBridge:profileSetOnce: {}", jsonString);
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId)
                    .profileSetOnce(JavaScriptUtils.jsObjectStrToJson(jsonString));
        }

        @JavascriptInterface
        public void profileUnset(String key) {
            log("_AppLogBridge:profileUnset: {}", key);
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).profileUnset(key);
        }

        @JavascriptInterface
        public void setHeaderInfo(String jsonString) {
            log("_AppLogBridge:setHeaderInfo: {}", jsonString);
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
                log("_AppLogBridge: wrong Json format", e);
                return;
            }
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).setHeaderInfo(map);
        }

        @JavascriptInterface
        public void addHeaderInfo(String key, String value) {
            log("_AppLogBridge:addHeaderInfo: {}, {}", key, value);
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).setHeaderInfo(key, value);
        }

        @JavascriptInterface
        public void removeHeaderInfo(String key) {
            log("_AppLogBridge:removeHeaderInfo: {}", key);
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId).removeHeaderInfo(key);
        }

        @JavascriptInterface
        public void onEventV3(@NonNull final String event, @Nullable final String params) {
            log("_AppLogBridge:onEventV3: {}, {}", event, params);
            AppLogHelper.getInstanceByAppIdOrGlobalDefault(appId)
                    .onEventV3(event, JavaScriptUtils.jsObjectStrToJson(params));
        }

        /**
         * 多实例下设置主实例AppId
         */
        @JavascriptInterface
        public void setNativeAppId(String appId) {
            log("_AppLogBridge:setNativeAppId: {}", appId);
            this.appId = (TextUtils.isEmpty(appId) || appId.equals("undefined")) ? null : appId;
        }

        private void log(String message, Object... args) {
            IAppLogLogger logger = AbstractAppLogLogger.getLogger(getAppId());
            if (null == logger) {
                logger = LoggerImpl.global();
            }
            logger.debug(loggerTags, message, args);
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    public static boolean injectH5Bridge(View webView, String url) {
        if (checkAllowList(url)) {
            LoggerImpl.global().debug(loggerTags, "injectH5Bridge to url:{}", url);
            JavaScriptUtils.addJavascriptInterface(webView, new _AppLogBridge(), "AppLogBridge");
            return true;
        } else {
            LoggerImpl.global()
                    .debug(loggerTags, "injectH5Bridge to url:{} not pass allowlist", url);
        }
        return false;
    }

    /**
     * 兼容旧的js sdk
     *
     * <p>适配 js sdk < 5.0 场景，5.0以后不需要执行该函数 TODO: 未来可以考虑去掉该兼容函数
     */
    public static void compatAppLogBridge(View view, String url) {
        LoggerImpl.global().debug(loggerTags, "Inject applog bridge compat js to: {}", view);
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
            LoggerImpl.global().debug(loggerTags, "checkAllowList url is empty");
            return false;
        }

        // 开启h5 bridge的实例
        List<AppLogInstance> enabledInstances =
                AppLogHelper.filterInstances(AppLogHelper.isH5BridgeEnabledMatcher);
        if (enabledInstances.isEmpty()) {
            LoggerImpl.global().debug(loggerTags, "No AppLog instance enabled h5 bridge");
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
            LoggerImpl.global().debug(loggerTags, "Parse host failed. url:" + url, e);
            return false;
        }

        if (TextUtils.isEmpty(host)) {
            // 无host的代码检查直接
            LoggerImpl.global().debug(loggerTags, "Host in url:{} is empty", url);
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
                LoggerImpl.global()
                        .error(loggerTags, "Check allow list by pattern:{} failed", e, w);
            }
        }
        return false;
    }
}
