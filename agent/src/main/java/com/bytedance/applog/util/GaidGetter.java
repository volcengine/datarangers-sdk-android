// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.content.Context;
import android.text.TextUtils;

import com.bytedance.applog.concurrent.TTExecutors;
import com.bytedance.applog.log.LoggerImpl;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.server.Api;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GaidGetter {
    private static volatile String sGaid = null;

    public static String getGaid(final Context context, final ConfigManager configManager) throws ExecutionException, InterruptedException, TimeoutException {
        return TTExecutors.getNormalExecutor().submit(new Callable<String>() {
            @Override
            public String call() {
                if (TextUtils.isEmpty(sGaid)) {
                    synchronized (GaidGetter.class) {
                        if (!TextUtils.isEmpty(sGaid)) {
                            return sGaid;
                        }

                        String gaid = null;
                        try {
                            com.google.android.gms.ads.identifier.AdvertisingIdClient.Info gpsAdInfo =
                                    com.google.android.gms.ads.identifier.AdvertisingIdClient
                                            .getAdvertisingIdInfo(context);

                            if (gpsAdInfo != null) {
                                gaid = gpsAdInfo.getId();
                            }
                        } catch (Throwable e) {
                            if (e instanceof ClassNotFoundException || e instanceof NoClassDefFoundError) {
                                // LogUtils.d(LogUtils.TAG, "没有依赖 google service，获取 gaid 失败");
                            } else {
                                LoggerImpl.global().error("Query Gaid failed", e);
                            }
                        }

                        if (TextUtils.isEmpty(gaid)) {
                            gaid = configManager.getStatSp().getString(Api.KEY_GOOGLE_AID, null);
                        } else {
                            String savedId = configManager.getStatSp().getString(Api.KEY_GOOGLE_AID, null);
                            if (!TextUtils.equals(savedId, gaid)) {
                                trySaveGoogleAid(gaid, configManager);
                            }
                        }
                        sGaid = gaid;
                    }
                }
                return sGaid;
            }
        }).get(configManager.getGaidTimeOutMilliSeconds(), TimeUnit.MILLISECONDS);
    }

    private static void trySaveGoogleAid(String gaid, ConfigManager configManager) {
        if (TextUtils.isEmpty(gaid) || configManager == null) {
            return;
        }
        configManager.getStatSp().edit().putString(Api.KEY_GOOGLE_AID, gaid).apply();
    }

    public static void clearSp(ConfigManager configManager) {
        if (configManager == null) {
            return;
        }
        configManager.getStatSp().edit().remove(Api.KEY_GOOGLE_AID).apply();
    }
}
