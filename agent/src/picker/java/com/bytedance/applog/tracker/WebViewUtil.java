// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.tracker;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.LruCache;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.bytedance.applog.AppLogHelper;
import com.bytedance.applog.log.LoggerImpl;
import com.bytedance.applog.util.ClassHelper;
import com.bytedance.applog.util.ReflectUtils;
import com.bytedance.applog.util.WebViewJsUtil;

import java.lang.reflect.Field;
import java.util.Collections;

/**
 * @author shiyanlong
 */
public class WebViewUtil {

    private static final LruCache<String, Long> mReportInjectSet = new LruCache<>(100);
    private static final LruCache<String, Long> mBridgeInjectSet = new LruCache<>(100);

    /**
     * 是否有实例开启了h5 bridge
     *
     * @return true 有
     */
    private static boolean hasAppLogInstanceH5BridgeEnabled() {
        return AppLogHelper.matchInstance(AppLogHelper.isH5BridgeEnabledMatcher);
    }

    /**
     * 是否有实例开启了h5 全埋点
     *
     * @return true 有
     */
    private static boolean hasAppLogInstanceH5CollectEnabled() {
        return AppLogHelper.matchInstance(AppLogHelper.isH5CollectEnabledMatcher);
    }

    private static boolean checkNeedInject(LruCache<String, Long> cache, View view) {
        if (view == null) {
            return false;
        }
        String key = view.hashCode() + "$$" + view.getId();
        return cache.get(key) == null;
    }

    private static void setWebViewInjected(LruCache<String, Long> cache, View view) {
        if (null == view) {
            return;
        }
        String key = view.hashCode() + "$$" + view.getId();
        cache.put(key, System.currentTimeMillis());
    }

    /**
     * 注入Bridge到webview中，如果只注入Bridge，则要求js sdk版本在5.0+
     *
     * @param webView WebView
     * @param url     webview load url
     */
    public static void injectWebViewBridges(View webView, String url) {
        boolean needClient = false;
        // AppLog Bridge注入
        if (checkNeedInject(mBridgeInjectSet, webView)) {
            if (hasAppLogInstanceH5BridgeEnabled()) {
                boolean injected = AppLogBridge.injectH5Bridge(webView, url);
                if (injected) {
                    setWebViewInjected(mBridgeInjectSet, webView);
                    needClient = true;
                }
            }
        }

        // NativeReport注入
        if (checkNeedInject(mReportInjectSet, webView)) {
            if (hasAppLogInstanceH5CollectEnabled()) {
                WebViewJsUtil.injectNativeReportCallback(webView);
                needClient = true;
                setWebViewInjected(mReportInjectSet, webView);
            }
        }

        // 如果注入任意Bridge，则需要添加WebChromeClient
        if (needClient) {
            addWebChromeClient(webView);
        }
    }

    /**
     * 注入JS到webview
     *
     * @param view WebView
     * @param url  webview load url
     */
    public static void injectWebViewJsCode(View view, String url) {
        AppLogBridge.compatAppLogBridge(view, url);
        if (hasAppLogInstanceH5CollectEnabled()) {
            WebViewJsUtil.injectCollectJs(view, url);
        }
    }

    /**
     * 添加WebChromeClient配置（仅当webview.getWebChromeClient()为null时）
     *
     * @param webView View
     */
    @SuppressLint("WebViewApiAvailability")
    private static void addWebChromeClient(View webView) {
        if (!ClassHelper.isWebView(webView)) {
            return;
        }
        try {
            WebChromeClient chromeClient = getWebChromeClient(((WebView) webView));
            if (null == chromeClient) {
                ((WebView) webView)
                        .setWebChromeClient(
                                new WebChromeClient() {

                                    @Override
                                    public void onProgressChanged(WebView view, int newProgress) {
                                        Tracker.onProgressChanged(this, view, newProgress);
                                        super.onProgressChanged(view, newProgress);
                                    }
                                });
            }
        } catch (Throwable e) {
            LoggerImpl.global()
                    .error(Collections.singletonList("WebViewUtil"), "getWebChromeClient failed", e);
        }
    }

    /**
     * 获取WebView已经设置的WebChromeClient
     *
     * @param webView WebView
     * @return WebChromeClient
     */
    @SuppressLint("WebViewApiAvailability")
    private static WebChromeClient getWebChromeClient(WebView webView) throws NoSuchFieldException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return webView.getWebChromeClient();
        }
        Object currentWeb = webView;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            try {
                currentWeb = ReflectUtils.getFieldValue(webView, "mProvider");
            } catch (Throwable e) {
                LoggerImpl.global()
                        .error(Collections.singletonList("WebViewUtil"), "Get provider failed", e);
            }
        }
        if (currentWeb == null) {
            throw new NoSuchFieldException("currentWeb is null");
        }
        WebChromeClient currentWebChromeClient = null;
        boolean reflectFailed = false; // 标记反射获取失败
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Object mClientAdapter =
                        ReflectUtils.getFieldValue(currentWeb, "mContentsClientAdapter");
                if (null != mClientAdapter) {
                    currentWebChromeClient =
                            (WebChromeClient)
                                    ReflectUtils.getFieldValue(mClientAdapter, "mWebChromeClient");
                } else {
                    reflectFailed = true;
                }
            } else {
                Object mCallbackProxy = ReflectUtils.getFieldValue(currentWeb, "mCallbackProxy");
                if (null != mCallbackProxy) {
                    currentWebChromeClient =
                            (WebChromeClient)
                                    ReflectUtils.getFieldValue(mCallbackProxy, "mWebChromeClient");
                } else {
                    reflectFailed = true;
                }
            }
            if (currentWebChromeClient == null) {
                LoggerImpl.global()
                        .debug(
                                Collections.singletonList("WebViewUtil"),
                                "Get webChromeClient failed, try to get it by type.");
                // OPPO 7.1.1 系统上源码是被混淆过得，反射会拿不到，这里兜底进行遍历属性判断类型获取
                Field[] fields = currentWeb.getClass().getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    Object value = field.get(currentWeb);
                    Field webChromeClientField = ReflectUtils.getFieldByType(value, WebChromeClient.class);
                    if (webChromeClientField != null) {
                        currentWebChromeClient = (WebChromeClient) webChromeClientField.get(value);
                        reflectFailed = false;
                        break;
                    }
                }
            }
        } catch (Throwable e) {
            LoggerImpl.global()
                    .error(
                            Collections.singletonList("WebViewUtil"),
                            "Get webChromeClient failed",
                            e);
        }
        if (reflectFailed) {
            // 如果是反射获取失败，WebChromeClient 实际可能不是 null，此时不能当做 null 处理，否则会覆盖用户设置的 WebChromeClient
            // 这里直接抛异常，外层捕获之后直接忽略不要 setWebChromeClient
            throw new NoSuchFieldException("WebChromeClient reflect failed");
        }
        return currentWebChromeClient;
    }
}
