// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.menu.ListMenuItemView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import java.lang.reflect.Method;

/**
 * @author shiyanlong
 */
public class ClassHelper {

    static boolean sCustomRecyclerView = false;

    private static Class<?> sCustomRecyclerViewClass;

    private static Method sCustomGetChildAdapterPositionMethod;

    private static final boolean sX5WebView;

    private static final boolean sSupportRecyclerView;

    private static final boolean sSupportViewPager;

    private static final boolean sSupportSwipeRefreshLayoutView;

    private static final boolean sSupportFragment;

    private static final boolean sSupportFragmentActivity;

    private static final boolean sSupportAlertDialog;

    private static final boolean sSupportListMenuItemView;

    private static final boolean sAndroidXRecyclerView;

    private static final boolean sAndroidXViewPager;

    private static final boolean sAndroidXSwipeRefreshLayoutView;

    private static final boolean sAndroidXFragment;

    private static final boolean sAndroidXFragmentActivity;

    private static final boolean sAndroidXAlertDialog;

    private static final boolean sAndroidXListMenuItemView;

    private static final boolean sTransform = false;

    static {
        if (!sTransform) {
            sX5WebView = hasClass("com.tencent.smtt.sdk.WebView");
            sSupportFragment = hasClass("android.support.v4.app.Fragment");
            sSupportFragmentActivity = hasClass("android.support.v4.app.FragmentActivity");
            sSupportAlertDialog = hasClass("android.support.v7.app.AlertDialog");
            sSupportViewPager = hasClass("android.support.v4.view.ViewPager");
            sSupportSwipeRefreshLayoutView =
                    hasClass("android.support.v4.widget.SwipeRefreshLayout");
            sSupportRecyclerView = hasClass("android.support.v7.widget.RecyclerView");
            sSupportListMenuItemView = hasClass("android.support.v7.view.menu.ListMenuItemView");
            sAndroidXFragment = hasClass("androidx.fragment.app.Fragment");
            sAndroidXFragmentActivity = hasClass("androidx.fragment.app.FragmentActivity");
            sAndroidXRecyclerView = hasClass("androidx.recyclerview.widget.RecyclerView");
            sAndroidXViewPager = hasClass("androidx.viewpager.widget.ViewPager");
            sAndroidXSwipeRefreshLayoutView =
                    hasClass("androidx.swiperefreshlayout.widget.SwipeRefreshLayout");
            sAndroidXAlertDialog = hasClass("androidx.appcompat.app.AlertDialog");
            sAndroidXListMenuItemView =
                    hasClass("androidx.appcompat.view.menu.ListMenuItemView");
        }
    }

    private static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    static int invokeCustomGetChildAdapterPositionMethod(View customRecyclerView, View childView) {
        try {
            if (customRecyclerView.getClass() == sCustomRecyclerViewClass) {
                return (int) sCustomGetChildAdapterPositionMethod.invoke(customRecyclerView, childView);
            }
        } catch (Throwable e) {
            TLog.ysnp(e);
        }
        return -1;
    }

    static void checkCustomRecyclerView(Class<?> viewClass, String viewName) {
        if (!sSupportRecyclerView && !sAndroidXRecyclerView && !sCustomRecyclerView) {
            if (viewName != null && viewName.contains("RecyclerView")) {
                try {
                    Class<?> clazz = findRecyclerInSuper(viewClass);
                    if (clazz != null && sCustomGetChildAdapterPositionMethod != null) {
                        sCustomRecyclerViewClass = viewClass;
                        sCustomRecyclerView = true;
                    }
                } catch (Throwable e) {
                    TLog.ysnp(e);
                }
            }
        }
    }

    private static Class<?> findRecyclerInSuper(Class<?> viewClass) {
        while (viewClass != null && !viewClass.equals(ViewGroup.class)) {
            try {
                sCustomGetChildAdapterPositionMethod =
                        viewClass.getDeclaredMethod("getChildAdapterPosition", View.class);
            } catch (Throwable e) {
            }
            if (sCustomGetChildAdapterPositionMethod == null) {
                try {
                    sCustomGetChildAdapterPositionMethod =
                            viewClass.getDeclaredMethod("getChildPosition", View.class);
                } catch (Throwable e) {
                }
            }
            if (sCustomGetChildAdapterPositionMethod != null) {
                return viewClass;
            }

            viewClass = viewClass.getSuperclass();
        }

        return null;
    }

    static boolean isRecyclerView(Object view) {
        return isAndroidXRecyclerView(view)
                || isSupportRecyclerView(view)
                || sCustomRecyclerView
                && view != null
                && sCustomRecyclerViewClass.isAssignableFrom(view.getClass());
    }

    static boolean isSupportRecyclerView(Object view) {
        return sSupportRecyclerView && view instanceof RecyclerView;
    }

    static boolean isAndroidXRecyclerView(Object view) {
        return sAndroidXRecyclerView
                && view instanceof androidx.recyclerview.widget.RecyclerView;
    }

    static boolean isSupportViewPager(Object view) {
        return sSupportViewPager && view instanceof ViewPager;
    }

    static boolean isAndroidXViewPager(Object view) {
        return sAndroidXViewPager && view instanceof androidx.viewpager.widget.ViewPager;
    }

    static boolean isSupportSwipeRefreshLayout(Object view) {
        return sSupportSwipeRefreshLayoutView && view instanceof SwipeRefreshLayout;
    }

    static boolean isAndroidXSwipeRefreshLayout(Object view) {
        return sAndroidXSwipeRefreshLayoutView
                && view instanceof androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
    }

    public static boolean isX5ChromeClient(Object client) {
        return sX5WebView && ReflectUtils.isInstance(client, "com.tencent.smtt.sdk.WebChromeClient");
    }

    public static boolean isSupportAlertDialog(Object dialog) {
        return sSupportAlertDialog && dialog instanceof AlertDialog;
    }

    public static boolean isSupportFragmentActivity(Object activity) {
        return sSupportFragmentActivity && activity instanceof FragmentActivity;
    }

    public static boolean isSupportFragment(Object fragment) {
        return sSupportFragment && fragment instanceof Fragment;
    }

    static boolean isSupportListMenuItemView(Object itemView) {
        return sSupportListMenuItemView && itemView instanceof ListMenuItemView;
    }

    public static boolean isAndroidXAlertDialog(Object dialog) {
        return sAndroidXAlertDialog && dialog instanceof androidx.appcompat.app.AlertDialog;
    }

    public static boolean isOfAndroidXFragmentActivity(Object activity) {
        return sAndroidXFragmentActivity
                && activity instanceof androidx.fragment.app.FragmentActivity;
    }

    public static boolean isAndroidXFragment(Object fragment) {
        return sAndroidXFragment && fragment instanceof androidx.fragment.app.Fragment;
    }

    static boolean isAndroidXListMenuItemView(Object itemView) {
        return sAndroidXListMenuItemView
                && itemView instanceof androidx.appcompat.view.menu.ListMenuItemView;
    }

    public static boolean isWebView(View view) {
        return view instanceof WebView || isX5WebView(view);
    }

    public static boolean isX5WebView(View view) {
        return sX5WebView && ReflectUtils.isInstance(view, "com.tencent.smtt.sdk.WebView");
    }
}
