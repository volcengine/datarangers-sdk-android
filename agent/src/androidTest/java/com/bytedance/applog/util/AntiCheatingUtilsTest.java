// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.bytedance.applog.AppLog;
import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.InitConfig;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.manager.DeviceManager;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AntiCheatingUtilsTest {

    @Test
    public void testAddAnticheatingHeader() {
        Context appContext = InstrumentationRegistry.getContext();
        final InitConfig config = new InitConfig("1234", "channel");
        final ConfigManager cfgManger = new ConfigManager(appContext, config);
        cfgManger.getStatSp().edit().clear().apply();
        DeviceManager manager =
                new DeviceManager((AppLogInstance) AppLog.getInstance(), appContext, cfgManger);
        JSONObject header = new JSONObject();
    }
}
