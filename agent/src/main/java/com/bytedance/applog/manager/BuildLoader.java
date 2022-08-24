// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;

import com.bytedance.applog.BuildConfig;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.RomUtils;
import com.bytedance.applog.util.SystemPropertiesWithCache;
import com.bytedance.applog.util.TLog;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author shiyanlong
 * @date 2019/2/2
 */
class BuildLoader extends BaseLoader {

    private final ConfigManager configManager;

    BuildLoader(Context context, ConfigManager configManager) {
        super(true, false);
        this.configManager = configManager;
    }

    @SuppressLint("MissingPermission")
    @Override
    protected boolean doLoad(final JSONObject info) throws JSONException {
        info.put(Api.KEY_PLATFORM, "Android");
        info.put(Api.KEY_SDK_LIB, "Android");
        info.put(Api.KEY_DEVICE_MODEL, Build.MODEL);
        info.put(Api.KEY_DEVICE_BRAND, Build.BRAND);
        info.put(Api.KEY_DEVICE_MANUFACTURER, Build.MANUFACTURER);
        info.put(Api.KEY_CPU_ABI, Build.CPU_ABI);
        info.put(Api.KEY_SDK_TARGET_VERSION, BuildConfig.SDK_TARGET_VERSION);
        info.put(Api.KEY_GIT_HASH, BuildConfig.GIT_HASH);

        if (RomUtils.isHarmonyUI()
                && null != configManager.getInitConfig()
                && configManager.getInitConfig().isHarmonyEnabled()) {
            // 加载鸿蒙系统信息
            info.put(Api.KEY_OS, "Harmony");
            try {
                info.put(
                        Api.KEY_OS_API, SystemPropertiesWithCache.get("hw_sc.build.os.apiversion"));
                info.put(
                        Api.KEY_OS_VERSION,
                        SystemPropertiesWithCache.get("hw_sc.build.platform.version"));
            } catch (Throwable e) {
                TLog.e("loadHarmonyInfo", e);
            }
        } else {
            info.put(Api.KEY_OS, "Android");
            info.put(Api.KEY_OS_API, Build.VERSION.SDK_INT);
            info.put(Api.KEY_OS_VERSION, Build.VERSION.RELEASE);
        }
        return true;
    }
}
