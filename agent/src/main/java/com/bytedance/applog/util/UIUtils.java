// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.DisplayMetrics;

/**
 * UI相关的工具类
 *
 * <p>fork from com.bytedance.component.silk.road:mohist-standard-tools:0.0.19
 *
 * @author luodong.seu
 */
public final class UIUtils {

    private static String sScreenResolution = "";
    private static int mDpi = -1;

    // 防止被继承
    private UIUtils() {}

    /**
     * 获取屏幕宽度
     *
     * @param context Context
     * @return int
     */
    public static int getScreenWidth(Context context) {
        if (context == null) {
            return 0;
        }

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return (dm == null) ? 0 : dm.widthPixels;
    }

    /**
     * 获取屏幕高度
     *
     * @param context Context
     * @return int
     */
    public static int getScreenHeight(Context context) {
        if (context == null) {
            return 0;
        }

        DisplayMetrics dm = context.getResources().getDisplayMetrics();

        return (dm == null) ? 0 : dm.heightPixels;
    }

    /**
     * 获取屏幕分辨率
     *
     * @param context Context
     * @return String w*h
     */
    public static String getScreenResolution(Context context) {
        if (TextUtils.isEmpty(sScreenResolution)) {
            if (context != null) {
                int width = getScreenWidth(context);
                int height = getScreenHeight(context);
                if (width > 0 && height > 0) {
                    sScreenResolution = width + "*" + height;
                }
            }
        }
        return sScreenResolution;
    }

    /**
     * 获取 DPI
     *
     * @param context Context
     * @return int
     */
    public static int getDpi(Context context) {
        if (mDpi == -1) {
            if (context != null) {
                mDpi =
                        context.getApplicationContext()
                                .getResources()
                                .getDisplayMetrics()
                                .densityDpi;
            }
        }
        return mDpi;
    }
}
