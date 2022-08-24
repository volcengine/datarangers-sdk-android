// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.tracker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Presentation;
import android.content.DialogInterface;
import android.location.Location;
import android.support.v4.app.Fragment;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bytedance.applog.AppLogHelper;
import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.collector.Navigator;
import com.bytedance.applog.store.Click;
import com.bytedance.applog.util.ClassHelper;
import com.bytedance.applog.util.TLog;
import com.bytedance.applog.util.ViewHelper;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author shiyanlong
 * @date 2019/1/16
 */
public class Tracker {

    private static float sRawX;
    private static float sRawY;
    private static int[] sLocation = new int[2];

    public static void onCheckedChanged(CompoundButton button, boolean isChecked) {
        onClick(button);
    }

    public static void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
        onClick(radioGroup.findViewById(radioGroup.getCheckedRadioButtonId()));
    }

    public static boolean onChildClick(
            ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        onClick(v);
        return false;
    }

    private static boolean hasBavEnabled() {
        boolean notEnabled =
                AppLogHelper.filterInstances(AppLogHelper.isBavEnabledMatcher).isEmpty();
        return !notEnabled;
    }

    public static void onClick(final View v) {
        if (v != null && hasBavEnabled()) {
            final Click vi = ViewHelper.getClickViewInfo(v, true);
            if (vi != null) {
                v.getLocationOnScreen(sLocation);
                int x = sLocation[0];
                int y = sLocation[1];
                int touchX = (int) (sRawX - x);
                int touchY = (int) (sRawY - y);
                if (touchX >= 0
                        && touchX <= v.getWidth()
                        && touchY >= 0
                        && touchY <= v.getHeight()) {
                    vi.touchX = touchX;
                    vi.touchY = touchY;
                }
                sRawX = 0;
                sRawY = 0;
                TLog.d(
                        "tracker:on click: width = "
                                + v.getWidth()
                                + " height = "
                                + v.getHeight()
                                + " touchX = "
                                + vi.touchX
                                + " touchY = "
                                + vi.touchY);
                AppLogHelper.handleAll(
                        new AppLogHelper.AppLogInstanceHandler() {
                            @Override
                            public void handle(AppLogInstance instance) {
                                if (!instance.isBavEnabled()) {
                                    return;
                                }
                                if (instance.isAutoTrackClickIgnored(v)) {
                                    return;
                                }
                                vi.properties = instance.getViewProperties(v);
                                instance.receive(vi.clone());
                            }
                        });
            } else {
                TLog.ysnp(null);
            }
        }
    }

    public static void onClick(DialogInterface dialog, int which) {
        if (dialog instanceof AlertDialog) {
            onClick(((AlertDialog) dialog).getButton(which));
        } else if (ClassHelper.isSupportAlertDialog(dialog)) {
            onClick(((android.support.v7.app.AlertDialog) dialog).getButton(which));
        } else if (ClassHelper.isAndroidXAlertDialog(dialog)) {
            onClick(((androidx.appcompat.app.AlertDialog) dialog).getButton(which));
        }
    }

    public static void onFocusChange(View view, boolean hasFocus) {
        if (view instanceof TextView) {
            onClick(view);
        }
    }

    public static boolean onGroupClick(
            ExpandableListView parent, View v, int groupPosition, long id) {
        onClick(v);
        return true;
    }

    public static void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        onClick(view);
    }

    public static void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        onItemClick(parent, view, position, id);
    }

    public static boolean onMenuItemClick(MenuItem item) {
        onClick(ViewHelper.getMenuItemView(item));
        return false;
    }

    public static void onOptionsItemSelected(MenuItem item) {
        onMenuItemClick(item);
    }

    public static void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
        if (fromUser) {
            onClick(ratingBar);
        }
    }

    public static void onStopTrackingTouch(SeekBar seekBar) {
        onClick(seekBar);
    }

    /* onResume */
    public static void onResume(Fragment fragment) {
        Navigator.onFragResume(fragment);
    }

    public static void onResume(android.app.Fragment fragment) {
        Navigator.onFragResume(fragment);
    }

    public static void onResume(android.webkit.WebViewFragment fragment) {
        Navigator.onFragResume(fragment);
    }

    public static void onResume(android.preference.PreferenceFragment fragment) {
        Navigator.onFragResume(fragment);
    }

    public static void onResume(android.app.ListFragment fragment) {
        Navigator.onFragResume(fragment);
    }

    public static void onResume(androidx.fragment.app.Fragment fragment) {
        Navigator.onFragResume(fragment);
    }

    /* onPause */
    public static void onPause(Fragment fragment) {
        Navigator.onFragPause(fragment);
    }

    public static void onPause(android.app.Fragment fragment) {
        Navigator.onFragPause(fragment);
    }

    public static void onPause(android.webkit.WebViewFragment fragment) {
        Navigator.onFragPause(fragment);
    }

    public static void onPause(android.preference.PreferenceFragment fragment) {
        Navigator.onFragPause(fragment);
    }

    public static void onPause(android.app.ListFragment fragment) {
        Navigator.onFragPause(fragment);
    }

    public static void onPause(androidx.fragment.app.Fragment fragment) {
        Navigator.onFragPause(fragment);
    }

    /* onHiddenChanged */
    public static void onHiddenChanged(Fragment fragment, boolean hidden) {
        if (hidden) {
            Navigator.onFragPause(fragment);
        } else {
            Navigator.onFragResume(fragment);
        }
    }

    public static void onHiddenChanged(android.app.Fragment fragment, boolean hidden) {
        if (hidden) {
            Navigator.onFragPause(fragment);
        } else {
            Navigator.onFragResume(fragment);
        }
    }

    public static void onHiddenChanged(android.webkit.WebViewFragment fragment, boolean hidden) {
        if (hidden) {
            Navigator.onFragPause(fragment);
        } else {
            Navigator.onFragResume(fragment);
        }
    }

    public static void onHiddenChanged(
            android.preference.PreferenceFragment fragment, boolean hidden) {
        if (hidden) {
            Navigator.onFragPause(fragment);
        } else {
            Navigator.onFragResume(fragment);
        }
    }

    public static void onHiddenChanged(android.app.ListFragment fragment, boolean hidden) {
        if (hidden) {
            Navigator.onFragPause(fragment);
        } else {
            Navigator.onFragResume(fragment);
        }
    }

    public static void onHiddenChanged(androidx.fragment.app.Fragment fragment, boolean hidden) {
        if (hidden) {
            Navigator.onFragPause(fragment);
        } else {
            Navigator.onFragResume(fragment);
        }
    }

    /* setUserVisibleHint */
    public static void setUserVisibleHint(Fragment fragment, boolean visible) {
        if (visible) {
            Navigator.onFragResume(fragment);
        } else {
            Navigator.onFragPause(fragment);
        }
    }

    public static void setUserVisibleHint(android.app.Fragment fragment, boolean visible) {
        if (visible) {
            Navigator.onFragResume(fragment);
        } else {
            Navigator.onFragPause(fragment);
        }
    }

    public static void setUserVisibleHint(
            android.webkit.WebViewFragment fragment, boolean visible) {
        if (visible) {
            Navigator.onFragResume(fragment);
        } else {
            Navigator.onFragPause(fragment);
        }
    }

    public static void setUserVisibleHint(
            android.preference.PreferenceFragment fragment, boolean visible) {
        if (visible) {
            Navigator.onFragResume(fragment);
        } else {
            Navigator.onFragPause(fragment);
        }
    }

    public static void setUserVisibleHint(android.app.ListFragment fragment, boolean visible) {
        if (visible) {
            Navigator.onFragResume(fragment);
        } else {
            Navigator.onFragPause(fragment);
        }
    }

    public static void setUserVisibleHint(
            androidx.fragment.app.Fragment fragment, boolean visible) {
        if (visible) {
            Navigator.onFragResume(fragment);
        } else {
            Navigator.onFragPause(fragment);
        }
    }

    public static boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return false;
    }

    public static void onLocationChanged(Location location) {}

    public static void onLongClick(View v) {}

    public static void show(Dialog dlg) {}

    public static void dismiss(Dialog dlg) {}

    public static void hide(Dialog dlg) {}

    /** webview打开url */
    public static void loadUrl(Object webView, String url) {
        if (null == webView) {
            return;
        }
        if (webView instanceof View && ClassHelper.isWebView((View) webView)) {
            WebViewUtil.injectWebViewBridges((View) webView, url);
        }
        try {
            Class<?> clazz = webView.getClass();
            Method method = clazz.getMethod("loadUrl", String.class);
            method.invoke(webView, url);
        } catch (Throwable e) {
            TLog.e(e);
        }
    }

    /** webview携带headers打开url */
    public static void loadUrl(Object webView, String url, Map<String, String> headers) {
        if (null == webView) {
            return;
        }
        if (webView instanceof View && ClassHelper.isWebView((View) webView)) {
            WebViewUtil.injectWebViewBridges((View) webView, url);
        }
        try {
            Class<?> clazz = webView.getClass();
            Method method = clazz.getMethod("loadUrl", String.class, Map.class);
            method.invoke(webView, url, headers);
        } catch (Throwable e) {
            TLog.e(e);
        }
    }

    /** webview loadData */
    public static void loadData(Object webView, String data, String mimeType, String encoding) {
        if (null == webView) {
            return;
        }
        if (webView instanceof View && ClassHelper.isWebView((View) webView)) {
            WebViewUtil.injectWebViewBridges((View) webView, "");
        }
        try {
            Class<?> clazz = webView.getClass();
            Method method = clazz.getMethod("loadData", String.class, String.class, String.class);
            method.invoke(webView, data, mimeType, encoding);
        } catch (Throwable e) {
            TLog.e(e);
        }
    }

    /** webview loadDataWithBaseURL */
    public static void loadDataWithBaseURL(
            Object webView,
            String baseUrl,
            String data,
            String mimeType,
            String encoding,
            String failUrl) {
        if (null == webView) {
            return;
        }
        if (webView instanceof View && ClassHelper.isWebView((View) webView)) {
            WebViewUtil.injectWebViewBridges((View) webView, baseUrl);
        }
        try {
            Class<?> clazz = webView.getClass();
            Method method =
                    clazz.getMethod(
                            "loadDataWithBaseURL",
                            String.class,
                            String.class,
                            String.class,
                            String.class,
                            String.class);
            method.invoke(webView, baseUrl, data, mimeType, encoding, failUrl);
        } catch (Throwable e) {
            TLog.e(e);
        }
    }

    /** 自动注入入口，在onProgressChanged时候可以做一些js的注入 https://juejin.cn/post/6844903567506014222 */
    public static void onProgressChanged(Object client, View view, int progress) {
        if (ClassHelper.isWebView(view)) {
            try {
                Class<?> clazz = view.getClass();
                Method method = clazz.getMethod("getUrl");
                Object url = method.invoke(view);
                if (null != url) {
                    String targetUrl = String.valueOf(url);
                    WebViewUtil.injectWebViewBridges(view, targetUrl);
                    WebViewUtil.injectWebViewJsCode(view, targetUrl);
                }
            } catch (Throwable e) {
                TLog.e(e);
            }
        }
    }

    public static void dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            TLog.d("tracker:enter dispatchTouchEvent");
            sRawX = ev.getRawX();
            sRawY = ev.getRawY();
        }
    }

    public static void onStart(Presentation presentation) {
        Navigator.onPresentationStart(presentation);
    }

    public static void onStop(Presentation presentation) {
        Navigator.onPresentationStop(presentation);
    }
}
