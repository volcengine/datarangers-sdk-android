// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.profile;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.log.LogInfo;
import com.bytedance.applog.network.INetworkClient;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.NetworkUtils;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/** 上报user_profile信息 Created by zhangxiaolong on 2018/5/16. */
public class UserProfileReporter implements Runnable {
    private final String url;
    private final String apiKey;
    private final JSONObject data;
    private final UserProfileCallback callback;
    private final Context context;
    private final AppLogInstance appLogInstance;
    private static final Handler mHandler = new Handler(Looper.getMainLooper());

    public UserProfileReporter(
            final AppLogInstance appLogInstance,
            String url,
            String apiKey,
            JSONObject data,
            UserProfileCallback callback,
            Context context) {
        this.appLogInstance = appLogInstance;
        this.url = url;
        this.apiKey = apiKey;
        this.data = data;
        this.callback = callback;
        this.context = context;
    }

    @Override
    public void run() {
        try {
            boolean hasNetwork = NetworkUtils.isNetworkAvailable(context);
            if (!hasNetwork) {
                reportFail(UserProfileCallback.NO_NET);
                return;
            }
            Map<String, String> m = new HashMap<String, String>();
            m.put("Content-Type", "application/json");
            m.put("X-APIKEY", apiKey);

            appLogInstance
                    .getNetClient()
                    .execute(
                            INetworkClient.METHOD_POST,
                            url,
                            data,
                            m,
                            INetworkClient.RESPONSE_TYPE_STRING,
                            false,
                            Api.HTTP_DEFAULT_TIMEOUT);
            reportSuccess();
        } catch (Throwable e) {
            appLogInstance
                    .getLogger()
                    .error(LogInfo.Category.USER_PROFILE, "Report profile failed", e);
            reportFail(UserProfileCallback.NET_ERROR);
        }
    }

    private void reportFail(final int code) {
        mHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onFail(code);
                        }
                    }
                });
    }

    private void reportSuccess() {
        mHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    }
                });
    }
}
