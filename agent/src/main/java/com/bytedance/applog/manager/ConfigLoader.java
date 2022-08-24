// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import android.content.Context;
import android.text.TextUtils;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.GaidGetter;
import com.bytedance.applog.util.TLog;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author shiyanlong
 * @date 2019/2/2
 **/
class ConfigLoader extends BaseLoader {

    private final Context mApp;

    private final ConfigManager mConfig;

    ConfigLoader(Context context, ConfigManager config) {
        super(false, false);
        mApp = context;
        mConfig = config;
    }

    @Override
    protected boolean doLoad(final JSONObject info) throws JSONException {
        info.put(Api.KEY_SDK_VERSION, TLog.SDK_VERSION);
        info.put(Api.KEY_SDK_VERSION_CODE, TLog.SDK_VERSION_CODE);
        info.put(Api.KEY_SDK_VERSION_NAME, TLog.SDK_VERSION_NAME);
        info.put(Api.KEY_CHANNEL, mConfig.getChannel());
        info.put(Api.KEY_NOT_REQUEST_SENDER, mConfig.getNotRequestSender() ? 1 : 0);
        DeviceManager.putString(info, Api.KEY_AID, mConfig.getAid());
        DeviceManager.putString(info, Api.KEY_RELEASE_BUILD, mConfig.getReleaseBuild());
        DeviceManager.putString(info, Api.KEY_USER_AGENT, mConfig.getUserAgent());
        DeviceManager.putString(info, Api.KEY_AB_SDK_VERSION, mConfig.getAbSdkVersion());

        String gaid = mConfig.getGoogleAID();
        if (TextUtils.isEmpty(gaid)) {
            gaid = GaidGetter.getGaid(mApp, mConfig);
        }
        DeviceManager.putString(info, Api.KEY_GOOGLE_AID, gaid);

        String appLanguage = mConfig.getAppLanguageFromInitConfig();
        if (TextUtils.isEmpty(appLanguage)) {
            appLanguage = mConfig.getAppLanguageFromSp();
        }
        DeviceManager.putString(info, Api.KEY_APP_LANGUAGE, appLanguage);
        String appRegion = mConfig.getAppRegionFromInitConfig();
        if (TextUtils.isEmpty(appRegion)) {
            appRegion = mConfig.getAppRegionFromSp();
        }
        DeviceManager.putString(info, Api.KEY_APP_REGION, appRegion);

        String appTrack = mConfig.getAppTrack();
        if (!TextUtils.isEmpty(appTrack)) {
            try {
                info.put(Api.KEY_APP_TRACK, new JSONObject(appTrack));
            } catch (Throwable t) {
                TLog.ysnp(t);
            }
        }

        String customInfo = mConfig.getCustomInfo();
        if (customInfo != null && customInfo.length() > 0) {
            try {
                JSONObject custom = new JSONObject(customInfo);
                custom.remove("_debug_flag");
                info.put(Api.KEY_CUSTOM, custom);
            } catch (Throwable t) {
                TLog.ysnp(t);
            }
        }
        String uuid = mConfig.getUserUniqueId();
        if (!TextUtils.isEmpty(uuid)) {
            DeviceManager.putString(info, Api.KEY_USER_UNIQUE_ID, uuid);
        }
        return true;
    }
}
