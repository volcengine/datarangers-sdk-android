// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.tracker;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.LruCache;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.bytedance.applog.AppLogHelper;
import com.bytedance.applog.util.ClassHelper;
import com.bytedance.applog.util.ReflectUtils;
import com.bytedance.applog.util.TLog;
import com.bytedance.applog.util.WebViewJsUtil;

/**
 * @author shiyanlong
 */
public class WebViewUtil {

    private static final LruCache<String, Long> mInjectSet = new LruCache<>(100);

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

    private static boolean checkNeedInject(View view) {
        if (view == null) {
            return false;
        }
        String key = view.hashCode() + "$$" + view.getId();
        if (mInjectSet.get(key) != null) {
            return false;
        }
        mInjectSet.put(key, System.currentTimeMillis());
        return true;
    }

    /**
     * 注入Bridge到webview中，如果只注入Bridge，则要求js sdk版本在5.0+
     *
     * @param webView WebView
     * @param url     webview load url
     */
    public static void injectWebViewBridges(View webView, String url) {
        if (checkNeedInject(webView)) {
            boolean needClient = false;
            if (hasAppLogInstanceH5BridgeEnabled()) {
                AppLogBridge.injectH5Bridge(webView, url);
                needClient = true;
            }
            if (hasAppLogInstanceH5CollectEnabled()) {
                WebViewJsUtil.injectNativeReportCallback(webView);
                needClient = true;
            }
            if (needClient) {
                addWebChromeClient(webView);
            }
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
        if (webView instanceof WebView) {
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
        }
    }

    /**
     * 获取WebView已经设置的WebChromeClient
     *
     * @param webView WebView
     * @return WebChromeClient
     */
    @SuppressLint("WebViewApiAvailability")
    private static WebChromeClient getWebChromeClient(WebView webView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return webView.getWebChromeClient();
        }
        Object currentWeb = webView;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            try {
                currentWeb = ReflectUtils.getFieldValue(webView, "mProvider");
            } catch (Throwable e) {
                TLog.e(e);
            }
        }
        WebChromeClient currentWebChromeClient = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Object mClientAdapter =
                        ReflectUtils.getFieldValue(currentWeb, "mContentsClientAdapter");
                if (null != mClientAdapter) {
                    currentWebChromeClient =
                            (WebChromeClient)
                                    ReflectUtils.getFieldValue(mClientAdapter, "mWebChromeClient");
                }
            } else {
                Object mCallbackProxy = ReflectUtils.getFieldValue(currentWeb, "mCallbackProxy");
                if (null != mCallbackProxy) {
                    currentWebChromeClient =
                            (WebChromeClient)
                                    ReflectUtils.getFieldValue(mCallbackProxy, "mWebChromeClient");
                }
            }
        } catch (Throwable e) {
            TLog.e(e);
        }
        return currentWebChromeClient;
    }
}
