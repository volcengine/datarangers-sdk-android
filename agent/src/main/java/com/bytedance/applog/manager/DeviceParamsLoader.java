// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import android.content.Context;

import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.HardwareUtils;
import com.bytedance.applog.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

public class DeviceParamsLoader extends BaseLoader {

    private final Context mApp;
    private final DeviceManager mManager;
    private final ConfigManager configManager;

    public DeviceParamsLoader(Context ctx, ConfigManager cmgr, DeviceManager mgr) {
        super(false, false);
        mApp = ctx;
        mManager = mgr;
        configManager = cmgr;
    }

    @Override
    protected boolean doLoad(final JSONObject info) throws JSONException, SecurityException {
        if (configManager.isOperatorInfoEnabled()) {
            final String carrier = HardwareUtils.getOperatorName(mApp);
            if (Utils.isNotEmpty(carrier)) {
                DeviceManager.putString(info, Api.KEY_CARRIER, carrier);
            }
            final String mccMnc = HardwareUtils.getOperatorMccMnc(mApp);
            if (Utils.isNotEmpty(mccMnc)) {
                DeviceManager.putString(info, Api.KEY_MCC_MNC, mccMnc);
            }
        }
        String clientUdid = mManager.getProvider().getClientUDID();
        DeviceManager.putString(info, Api.KEY_C_UDID, clientUdid);

        String openUdid = mManager.getProvider().getOpenUdid();
        DeviceManager.putString(info, Api.KEY_OPEN_UDID, openUdid);
        return true;
    }

    @Override
    protected String getName() {
        return "DeviceParams";
    }
}
