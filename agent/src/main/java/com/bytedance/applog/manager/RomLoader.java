// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import android.os.Build;
import android.text.TextUtils;

import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.RomUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author shiyanlong
 * @date 2019/2/2
 */
class RomLoader extends BaseLoader {

    RomLoader() {
        super(true, false);
    }

    @Override
    protected boolean doLoad(final JSONObject info) throws JSONException {
        StringBuilder rom = new StringBuilder(16);
        if (RomUtils.isMiui()) {
            rom.append("MIUI-");
        } else if (RomUtils.isFlyme()) {
            rom.append("FLYME-");
        } else {
            String emuiInfo = RomUtils.getEmuiInfo();
            if (RomUtils.isHwOrHonor(emuiInfo)) {
                rom.append("EMUI-");
            }
            if (!TextUtils.isEmpty(emuiInfo)) {
                rom.append(emuiInfo).append("-");
            }
        }
        rom.append(Build.VERSION.INCREMENTAL);
        info.put(Api.KEY_ROM, rom.toString());

        String romVersion = RomUtils.getRomInfo();
        if (!TextUtils.isEmpty(romVersion)) {
            info.put(Api.KEY_ROM_VERSION, romVersion);
        }
        return true;
    }
}
