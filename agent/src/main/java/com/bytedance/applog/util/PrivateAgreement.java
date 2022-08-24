// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.bytedance.applog.BuildConfig;
import com.bytedance.applog.concurrent.TTExecutors;
import com.bytedance.applog.store.SharedPreferenceCacheHelper;

public class PrivateAgreement {
    private static final String KEY_STARTED = "_install_started_v2";
    private static final String SP_INSTALL = "ug_install_settings_pref";
    private static boolean sAccepted;

    public static boolean hasAccept(Context context) {
        // 海外没有要求
        if (BuildConfig.IS_I18N) {
            return true;
        }
        if (context == null) {
            return false;
        }
        if (sAccepted) {
            return true;
        }
        // 如果上次已经初始化过了，也代表已经同意隐私弹窗
        SharedPreferences sp = getPrivateInstallSp(context);
        return sp.getBoolean(KEY_STARTED, false);
    }

    private static AbsSingleton<SharedPreferences> sInstallSpRef =
            new AbsSingleton<SharedPreferences>() {
                @Override
                protected SharedPreferences create(Object... params) {
                    return SharedPreferenceCacheHelper.getSafeSharedPreferences(
                            ((Context) params[0]), SP_INSTALL, Context.MODE_PRIVATE);
                }
            };

    /**
     *
     * @param context context
     * @return sp
     */
    private static SharedPreferences getPrivateInstallSp(Context context) {
        return sInstallSpRef.get(context);
    }

    public static void setAccepted(final Context context) {
        sAccepted = true;

        // 异步写SP：解决耗时问题
        TTExecutors.getNormalExecutor()
                .submit(
                        new Runnable() {
                            @Override
                            public void run() {
                                sInstallSpRef
                                        .get(context)
                                        .edit()
                                        .putBoolean(KEY_STARTED, true)
                                        .apply();
                            }
                        });
    }
}
