// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import android.annotation.SuppressLint;
import android.content.Context;

import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.NetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author shiyanlong
 * @date 2019/2/2
 */
class NetLoader extends BaseLoader {

    private final Context mApp;

    NetLoader(Context ctx) {
        super(true, true);
        mApp = ctx;
    }

    @SuppressLint("MissingPermission")
    @Override
    protected boolean doLoad(final JSONObject info) throws JSONException {
        String access = NetworkUtils.getNetworkAccessType(mApp);

        // TODO: 2019/4/12 header字段中的网络状况，应该刷新。
        DeviceManager.putString(info, Api.KEY_ACCESS, access);
        return true;
    }
}
