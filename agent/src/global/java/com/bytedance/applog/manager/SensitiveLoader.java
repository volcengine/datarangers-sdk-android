// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import android.content.Context;

import com.bytedance.applog.ISensitiveInfoProvider;

import org.json.JSONObject;

/**
 * @author shiyanlong
 * @date 2019/2/2
 */
class SensitiveLoader extends BaseLoader {

    SensitiveLoader(
            Context ctx,
            ConfigManager cfg,
            DeviceManager deviceManager,
            ISensitiveInfoProvider sensitiveInfoProvider) {
        super(true, false);
    }

    @Override
    protected boolean doLoad(final JSONObject info) {
        return true;
    }
}
