// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.text.TextUtils;

import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.TLog;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author shiyanlong
 * @date 2019/2/2
 */
class PackageLoader extends BaseLoader {

    private final Context mApp;

    private final ConfigManager mConfig;

    PackageLoader(Context ctx, ConfigManager configManager) {
        super(false, false);
        mApp = ctx;
        mConfig = configManager;
    }

    @Override
    protected boolean doLoad(final JSONObject info) throws JSONException {
        String pkg = mApp.getPackageName();
        if (TextUtils.isEmpty(mConfig.getZiJiePkg())) {
            info.put(Api.KEY_PACKAGE, pkg);
        } else {
            TLog.d("has zijie pkg");
            info.put(Api.KEY_PACKAGE, mConfig.getZiJiePkg());
            info.put(Api.KEY_REAL_PACKAGE_NAME, pkg);
        }

        try {
            PackageInfo packageInfo = mApp.getPackageManager().getPackageInfo(pkg, 0);
            int versionCode = packageInfo.versionCode;

            if (!TextUtils.isEmpty(mConfig.getVersion())) {
                info.put(Api.KEY_APP_VERSION, mConfig.getVersion());
            } else {
                info.put(Api.KEY_APP_VERSION, packageInfo.versionName);
            }

            if (!TextUtils.isEmpty(mConfig.getVersionMinor())) {
                info.put(Api.KEY_APP_VERSION_MINOR, mConfig.getVersionMinor());
            } else {
                info.put(Api.KEY_APP_VERSION_MINOR, "");
            }

            if (mConfig.getVersionCode() != 0) {
                info.put(Api.KEY_VERSION_CODE, mConfig.getVersionCode());
            } else {
                info.put(Api.KEY_VERSION_CODE, versionCode);
            }

            if (mConfig.getUpdateVersionCode() != 0) {
                info.put(Api.KEY_UPDATE_VERSION_CODE, mConfig.getUpdateVersionCode());
            } else {
                info.put(Api.KEY_UPDATE_VERSION_CODE, versionCode);
            }

            if (mConfig.getManifestVersionCode() != 0) {
                info.put(Api.KEY_MANIFEST_VERSION_CODE, mConfig.getManifestVersionCode());
            } else {
                info.put(Api.KEY_MANIFEST_VERSION_CODE, versionCode);
            }

            if (!TextUtils.isEmpty(mConfig.getAppName())) {
                info.put(Api.KEY_APP_NAME, mConfig.getAppName());
            }
            if (!TextUtils.isEmpty(mConfig.getTweakedChannel())) {
                info.put(Api.KEY_TWEAKED_CHANNEL, mConfig.getTweakedChannel());
            }

            if (packageInfo.applicationInfo != null) {
                int resId = packageInfo.applicationInfo.labelRes;
                if (resId > 0) {
                    info.put(Api.KEY_DISPLAY_NAME, mApp.getString(resId));
                }
            }
            return true;
        } catch (Throwable e) {
            TLog.ysnp(e);
            return false;
        }
    }
}
