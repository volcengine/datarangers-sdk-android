// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.bytedance.applog.ISensitiveInfoProvider;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.SensitiveUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author shiyanlong
 * @date 2019/2/2
 */
class SensitiveLoader extends BaseLoader {

    private final DeviceManager mManager;

    SensitiveLoader(DeviceManager deviceManager) {
        super(true, false);
        mManager = deviceManager;
    }

    @SuppressLint("HardwareIds")
    @Override
    protected boolean doLoad(final JSONObject info) throws JSONException {

        String udid = mManager.getProvider().getUdId();
        DeviceManager.putString(info, Api.KEY_UDID, udid);

        JSONArray udidList = mManager.getProvider().getUdIdList();
        if (SensitiveUtils.validMultiImei(udidList)) {
            info.put(Api.KEY_UDID_LIST, udidList);
        }

        String serialNumber = mManager.getProvider().getSerialNumber();
        DeviceManager.putString(info, Api.KEY_SERIAL_NUMBER, serialNumber);

        return true;
    }

    @Override
    protected String getName() {
        return "SensitiveLoader";
    }
}
