// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import android.content.Context;
import android.telephony.TelephonyManager;
import com.bytedance.applog.server.Api;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author shiyanlong
 * @date 2019/2/2
 **/
class SimCountryLoader extends BaseLoader {

    private final Context mApp;

    SimCountryLoader(Context ctx) {
        super(true, false);
        mApp = ctx;
    }

    @Override
    protected boolean doLoad(final JSONObject info) throws JSONException {
        String simRegion = ((TelephonyManager) mApp.getSystemService(Context.TELEPHONY_SERVICE))
                .getSimCountryIso();
        DeviceManager.putString(info, Api.KEY_SIM_REGION, simRegion);
        return true;
    }
}
