// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.annotation.TargetApi;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.bytedance.applog.log.LoggerImpl;

import java.lang.reflect.Method;

/**
 * @author shiyanlong
 */
public class ClassHelper {

    static boolean sHasCustomRecyclerView = false;

    private static Class sCRVClass;

    private static Method sCRVGetChildAdapterPositionMethod;

    private static boolean sHasX5WebView;

    private static boolean sHasSupportRecyclerView;

    private static boolean sHasSupportViewPager;

    private static boolean sHasSupportSwipeRefreshLayoutView;

    private static boolean sHasSupportFragment;

    private static boolean sHasSupportFragmentActivity;

    private static boolean sHasSupportAlertDialog;

    private static boolean sHasSupportListMenuItemView;

    private static boolean sHasAndroidXRecyclerView;

    private static boolean sHasAndroidXViewPager;

    private static boolean sHasAndroidXSwipeRefreshLayoutView;

    private static boolean sHasAndroidXFragment;

    private static boolean sHasAndroidXFragmentActivity;

    private static boolean sHasAndroidXAlertDialog;

    private static boolean sHasAndroidXListMenuItemView;

    private static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Throwable var2) {
            return false;
        }
    }

    static int invokeCRVGetChildAdapterPositionMethod(View customRecyclerView, View childView) {
        try {
            if (customRecyclerView.getClass() == sCRVClass) {
                return (Integer)
                        sCRVGetChildAdapterPositionMethod.invoke(customRecyclerView, childView);
            }
        } catch (Throwable var3) {
            LoggerImpl.global().error("invokeCRVGetChildAdapterPositionMethod failed", var3);
        }

        return -1;
    }

    @TargetApi(9)
    static void checkCustomRecyclerView(Class<?> viewClass, String viewName) {
        if (!sHasAndroidXRecyclerView && !sHasSupportRecyclerView && !sHasCustomRecyclerView) {
            if (viewName != null && viewName.contains("RecyclerView")) {
                try {
                    Class<?> rootCRVClass = findRecyclerInSuper(viewClass);
                    if (rootCRVClass != null && sCRVGetChildAdapterPositionMethod != null) {
                        sCRVClass = viewClass;
                        sHasCustomRecyclerView = true;
                    }
                } catch (Exception var3) {
                    LoggerImpl.global().error("checkCustomRecyclerView failed", var3);
                }
            }
        }
    }

    private static Class<?> findRecyclerInSuper(Class<?> viewClass) {
        while (viewClass != null && !viewClass.equals(ViewGroup.class)) {
            try {
                sCRVGetChildAdapterPositionMethod =
                        viewClass.getDeclaredMethod("getChildAdapterPosition", View.class);
            } catch (NoSuchMethodException ignore) {
            }

            if (sCRVGetChildAdapterPositionMethod == null) {
                try {
                    sCRVGetChildAdapterPositionMethod =
                            viewClass.getDeclaredMethod("getChildPosition", View.class);
                } catch (NoSuchMethodException ignore) {
                }
            }

            if (sCRVGetChildAdapterPositionMethod != null) {
                return viewClass;
            }

            viewClass = viewClass.getSuperclass();
        }

        return null;
    }

    static boolean instanceOfRecyclerView(Object view) {
        return instanceOfAndroidXRecyclerView(view)
                || instanceOfSupportRecyclerView(view)
                || sHasCustomRecyclerView
                        && view != null
                        && sCRVClass.isAssignableFrom(view.getClass());
    }

    static boolean instanceOfSupportRecyclerView(Object view) {
        return sHasSupportRecyclerView && ReflectUtils.isInstance(view, "android.support.v7.widget.RecyclerView");
    }

    static boolean instanceOfAndroidXRecyclerView(Object view) {
        return sHasAndroidXRecyclerView
                && view instanceof androidx.recyclerview.widget.RecyclerView;
    }

    static boolean instanceOfSupportViewPager(Object view) {
        return sHasSupportViewPager && ReflectUtils.isInstance(view, "android.support.v4.view.ViewPager");
    }

    static boolean instanceOfAndroidXViewPager(Object view) {
        return sHasAndroidXViewPager && view instanceof androidx.viewpager.widget.ViewPager;
    }

    static boolean instanceOfSupportSwipeRefreshLayout(Object view) {
        return sHasSupportSwipeRefreshLayoutView && ReflectUtils.isInstance(view, "android.support.v4.widget.SwipeRefreshLayout");
    }

    static boolean instanceofAndroidXSwipeRefreshLayout(Object view) {
        return sHasAndroidXSwipeRefreshLayoutView
                && view instanceof androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
    }

    public static boolean isWebView(View view) {
        return view instanceof WebView || isX5WebView(view);
    }

    public static boolean isX5WebView(View view) {
        return sHasX5WebView &&  ReflectUtils.isInstance(view, "com.tencent.smtt.sdk.WebView");
    }

    public static boolean isX5ChromeClient(Object client) {
        return sHasX5WebView && ReflectUtils.isInstance(client, "com.tencent.smtt.sdk.WebChromeClient");
    }

    public static boolean instanceOfSupportAlertDialog(Object dialog) {
        return sHasSupportAlertDialog && ReflectUtils.isInstance(dialog, "android.support.v7.app.AlertDialog");
    }

    public static boolean instanceOfAndroidXAlertDialog(Object dialog) {
        return sHasAndroidXAlertDialog && dialog instanceof androidx.appcompat.app.AlertDialog;
    }

    public static boolean instanceOfSupportFragmentActivity(Object activity) {
        return sHasSupportFragmentActivity && ReflectUtils.isInstance(activity, "android.support.v4.app.FragmentActivity");
    }

    public static boolean instanceOfAndroidXFragmentActivity(Object activity) {
        return sHasAndroidXFragmentActivity
                && activity instanceof androidx.fragment.app.FragmentActivity;
    }

    public static boolean instanceOfSupportFragment(Object fragment) {
        return sHasSupportFragment && ReflectUtils.isInstance(fragment, "android.support.v4.app.Fragment");
    }

    public static boolean instanceOfAndroidXFragment(Object fragment) {
        return sHasAndroidXFragment && fragment instanceof androidx.fragment.app.Fragment;
    }

    static boolean instanceOfSupportListMenuItemView(Object itemView) {
        return sHasSupportListMenuItemView && ReflectUtils.isInstance(itemView, "android.support.v7.view.menu.ListMenuItemView");
    }

    static boolean instanceOfAndroidXListMenuItemView(Object itemView) {
        return sHasAndroidXListMenuItemView
                && itemView instanceof androidx.appcompat.view.menu.ListMenuItemView;
    }

    public static void dumpClassInfo() {
        String info =
                "Support classes: \nsHasSupportRecyclerView="
                        + sHasSupportRecyclerView
                        + ", sHasSupportFragmentActivity="
                        + sHasSupportFragmentActivity
                        + "\nsHasSupportFragment="
                        + sHasSupportFragment
                        + ", sHasSupportAlertDialog="
                        + sHasSupportAlertDialog
                        + "\nsHasSupportSwipeRefreshLayoutView="
                        + sHasSupportSwipeRefreshLayoutView
                        + ", sHasSupportViewPager="
                        + sHasSupportViewPager
                        + "\nsHasSupportListMenuItemView="
                        + sHasSupportListMenuItemView
                        + "\nFor AndroidX Class: \nsHasAndroidXRecyclerView="
                        + sHasAndroidXRecyclerView
                        + ", sHasAndroidXFragmentActivity="
                        + sHasAndroidXFragmentActivity
                        + "\nsHasAndroidXFragment="
                        + sHasAndroidXFragment
                        + ", sHasAndroidXAlertDialog="
                        + sHasAndroidXAlertDialog
                        + "\nsHasAndroidXSwipeRefreshLayoutView="
                        + sHasAndroidXSwipeRefreshLayoutView
                        + ", sHasAndroidXViewPager="
                        + sHasAndroidXViewPager
                        + "\nsHasAndroidXListMenuItemView="
                        + sHasAndroidXListMenuItemView
                        + "\nAnd sHasTransform="
                        + sHasTransform;
        LoggerImpl.global().debug(info);
    }

    private static final boolean sHasTransform = false;

    static {
        if (!sHasTransform) {
            sHasX5WebView = hasClass("com.tencent.smtt.sdk.WebView");
            sHasSupportRecyclerView = hasClass("android.support.v7.widget.RecyclerView");
            sHasSupportViewPager = hasClass("android.support.v4.view.ViewPager");
            sHasSupportSwipeRefreshLayoutView =
                    hasClass("android.support.v4.widget.SwipeRefreshLayout");
            sHasSupportFragment = hasClass("android.support.v4.app.Fragment");
            sHasSupportFragmentActivity = hasClass("android.support.v4.app.FragmentActivity");
            sHasSupportAlertDialog = hasClass("android.support.v7.app.AlertDialog");
            sHasSupportListMenuItemView = hasClass("android.support.v7.view.menu.ListMenuItemView");
            sHasAndroidXRecyclerView = hasClass("androidx.recyclerview.widget.RecyclerView");
            sHasAndroidXViewPager = hasClass("androidx.viewpager.widget.ViewPager");
            sHasAndroidXSwipeRefreshLayoutView =
                    hasClass("androidx.swiperefreshlayout.widget.SwipeRefreshLayout");
            sHasAndroidXFragment = hasClass("androidx.fragment.app.Fragment");
            sHasAndroidXFragmentActivity = hasClass("androidx.fragment.app.FragmentActivity");
            sHasAndroidXAlertDialog = hasClass("androidx.appcompat.app.AlertDialog");
            sHasAndroidXListMenuItemView =
                    hasClass("androidx.appcompat.view.menu.ListMenuItemView");
            //            dumpClassInfo();
        }
    }
}
