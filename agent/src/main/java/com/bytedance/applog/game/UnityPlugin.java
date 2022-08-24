// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.game;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;

import com.bytedance.applog.AppLog;
import com.bytedance.applog.ILogger;
import com.bytedance.applog.InitConfig;
import com.bytedance.applog.UriConfig;
import com.bytedance.applog.util.TLog;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author: liujunlin
 * @date: 2021/5/7
 *     <p>TODO: 支持多实例
 */
public class UnityPlugin {
    private Activity unityActivity;

    private Activity getActivity() {
        if (unityActivity == null) {
            try {
                Class<?> classtype = Class.forName("com.unity3d.player.UnityPlayer");
                unityActivity =
                        (Activity) classtype.getDeclaredField("currentActivity").get(classtype);
            } catch (Exception e) {

            }
        }
        return unityActivity;
    }

    public void onEventV3(String event, String params) {
        try {
            AppLog.onEventV3(event, new JSONObject(params));
        } catch (JSONException e) {
            TLog.e(e);
        }
    }

    public void init(
            String appId,
            String channel,
            boolean enableAb,
            boolean enableEncrypt,
            boolean enableLog,
            String host) {
        InitConfig initConfig = new InitConfig(appId, channel);
        initConfig.setAbEnable(enableAb);
        AppLog.setEncryptAndCompress(enableEncrypt);
        if (enableLog) {
            initConfig.setLogger(
                    new ILogger() {
                        @Override
                        public void log(String msg, Throwable t) {
                            Log.d("AppLog", msg, t);
                        }
                    });
        }
        if (!TextUtils.isEmpty(host)) {
            initConfig.setUriConfig(UriConfig.createByDomain(host, null));
        }
        AppLog.init(getActivity(), initConfig);
    }

    public void setCustomHeaderInfo(String key, String value) {
        AppLog.setHeaderInfo(key, value);
    }

    public void removeCustomHeaderInfo(String key) {
        AppLog.removeHeaderInfo(key);
    }

    public void profileSet(String params) {
        try {
            AppLog.profileSet(new JSONObject(params));
        } catch (JSONException e) {
            TLog.e(e);
        }
    }

    public void profileAppend(String params) {
        try {
            AppLog.profileAppend(new JSONObject(params));
        } catch (JSONException e) {
            TLog.e(e);
        }
    }

    public void profileSetOnce(String params) {
        try {
            AppLog.profileSetOnce(new JSONObject(params));
        } catch (JSONException e) {
            TLog.e(e);
        }
    }

    public void profileUnset(String key) {
        AppLog.profileUnset(key);
    }

    public void profileIncrement(String params) {
        try {
            AppLog.profileIncrement(new JSONObject(params));
        } catch (JSONException e) {
            TLog.e(e);
        }
    }

    public String getAbSdkVersion() {
        return AppLog.getAbSdkVersion();
    }

    public String getAllAbTestConfigs() {
        return AppLog.getAllAbTestConfigs().toString();
    }

    public String getAbConfig(String key, String value) {
        return AppLog.getAbConfig(key, value);
    }

    public void setExternalAbVersion(String externalAbVersion) {
        AppLog.setExternalAbVersion(externalAbVersion);
    }

    public String getDeviceId() {
        return AppLog.getDid();
    }

    public String getSsid() {
        return AppLog.getSsid();
    }

    public String getIid() {
        return AppLog.getIid();
    }

    public void flush() {
        AppLog.flush();
    }

    public void setUserUniqueID(String userUniqueID) {
        AppLog.setUserUniqueID(userUniqueID);
    }

    public String getUserUniqueID() {
        return AppLog.getUserUniqueID();
    }

    public String getAid() {
        return AppLog.getAid();
    }
}
