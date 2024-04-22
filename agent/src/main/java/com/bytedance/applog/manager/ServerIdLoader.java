// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import android.content.Context;
import android.content.SharedPreferences;

import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author shiyanlong
 * @date 2019/2/2
 */
class ServerIdLoader extends BaseLoader {

    private final Context mContext;
    private final ConfigManager mConfig;
    private final DeviceManager mManager;

    ServerIdLoader(Context context, ConfigManager cfg, DeviceManager deviceManager) {
        // 本次不开启sub进程同步逻辑，先设置false
        super(true, false, false);
        mContext = context;
        mConfig = cfg;
        mManager = deviceManager;
    }

    @Override
    protected boolean doLoad(final JSONObject info) throws JSONException {
        final SharedPreferences sp = mConfig.getStatSp();

        String did = sp.getString(Api.KEY_DEVICE_ID, null);
        DeviceManager.putString(info, Api.KEY_DEVICE_ID, did);

        String bdDid = sp.getString(Api.KEY_BD_DID, null);
        DeviceManager.putString(info, Api.KEY_BD_DID, bdDid);

        String installId = sp.getString(Api.KEY_INSTALL_ID, null);
        String ssid = sp.getString(mConfig.getSsidSpKey(), null);

        DeviceManager.putString(info, Api.KEY_INSTALL_ID, installId);
        DeviceManager.putString(info, Api.KEY_SSID, ssid);

        long registerTime = sp.getLong(Api.KEY_REGISTER_TIME, 0L);
        if (!Utils.checkId(installId) || !(Utils.checkId(did) || Utils.checkId(bdDid)) || !Utils.checkId(ssid)) {
            if (registerTime != 0) {
                registerTime = 0;
                mConfig.updateRegisterTime(0L);
            }
        }
        info.put(Api.KEY_REGISTER_TIME, registerTime);
        return true;
    }

    @Override
    protected String getName() {
        return "ServerId";
    }
}
