// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.profile;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.bytedance.applog.IAppLogInstance;
import com.bytedance.applog.util.NetworkUtils;
import com.bytedance.applog.util.TLog;

import java.util.HashMap;
import java.util.Map;

/** 上报user_profile信息 Created by zhangxiaolong on 2018/5/16. */
public class UserProfileReporter implements Runnable {
    private final String url;
    private final String apiKey;
    private final String data;
    private final UserProfileCallback callback;
    private final Context context;
    private final IAppLogInstance appLogInstance;
    private static final Handler mHandler = new Handler(Looper.getMainLooper());

    public UserProfileReporter(
            final IAppLogInstance appLogInstance,
            String url,
            String apiKey,
            String data,
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

            appLogInstance.getNetClient().post(url, data.getBytes(), m);
            reportSuccess();
        } catch (Throwable e) {
            TLog.e(e);
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
