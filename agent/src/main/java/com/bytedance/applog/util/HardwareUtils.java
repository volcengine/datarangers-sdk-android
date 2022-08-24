// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.Display;
import android.view.WindowManager;

import com.bytedance.applog.store.SharedPreferenceCacheHelper;

public class HardwareUtils {

    public static final String GLOBAL_CACHE_GET_ANDROID_ID = "Secure.getString_android_id";

    @SuppressLint("HardwareIds")
    public static String getSecureAndroidId(final Context context) {
        try {
            return SharedPreferenceCacheHelper.getGlobal(context)
                    .getOrLoad(
                            GLOBAL_CACHE_GET_ANDROID_ID,
                            new SharedPreferenceCacheHelper.CacheLoader() {
                                @Override
                                public String load() throws Throwable {
                                    TLog.d(
                                            "[DeviceMeta] Try to get android id by secure.getString.");
                                    return Settings.Secure.getString(
                                            context.getContentResolver(),
                                            Settings.Secure.ANDROID_ID);
                                }
                            });
        } catch (Throwable e) {
            TLog.e(e);
            return null;
        }
    }

    /**
     * 获取屏幕方向
     *
     * @param context Context
     * @return 0:未知 1:ORIENTATION_PORTRAIT 2:ORIENTATION_LANDSCAPE
     */
    public static int getScreenOrientation(final Context context) {
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int orientation = Configuration.ORIENTATION_UNDEFINED;
        if (null != windowManager) {
            Display display = windowManager.getDefaultDisplay();
            if (null != display) {
                if (display.getWidth() <= display.getHeight()) {
                    orientation = Configuration.ORIENTATION_PORTRAIT;
                } else {
                    orientation = Configuration.ORIENTATION_LANDSCAPE;
                }
            }
        }
        return orientation;
    }

    /**
     * 获取运营商名称
     *
     * @param context Context
     * @return 名称|null
     */
    public static String getOperatorName(final Context context) {
        TelephonyManager mgr =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (mgr != null) {
            return mgr.getNetworkOperatorName();
        }
        return null;
    }

    /**
     * 获取运营商信息
     *
     * @param context Context
     * @return mcc_mnc
     */
    public static String getOperatorMccMnc(final Context context) {
        TelephonyManager mgr =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (mgr != null) {
            return mgr.getNetworkOperator();
        }
        return null;
    }
}
