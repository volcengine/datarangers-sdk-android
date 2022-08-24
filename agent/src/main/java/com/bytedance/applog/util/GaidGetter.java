// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.content.Context;
import android.text.TextUtils;

import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.server.Api;

public class GaidGetter {
    private static volatile String sGaid = null;

    public static String getGaid(Context context, ConfigManager configManager) {
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
                        TLog.e(e);
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
