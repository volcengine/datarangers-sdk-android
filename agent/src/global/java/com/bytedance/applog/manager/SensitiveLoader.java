// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import org.json.JSONObject;

/**
 * @author shiyanlong
 * @date 2019/2/2
 */
class SensitiveLoader extends BaseLoader {

    SensitiveLoader(DeviceManager deviceManager) {
        super(true, false);
    }

    @Override
    protected boolean doLoad(final JSONObject info) {
        return true;
    }

    @Override
    protected String getName() {
        return "SensitiveLoader";
    }
}
