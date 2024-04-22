// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import android.Manifest;
import android.content.Context;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import android.util.Log;
import com.bytedance.applog.AppLog;
import com.bytedance.applog.BaseAppLogTest;
import com.bytedance.applog.InitConfig;
import com.bytedance.applog.util.ReflectUtils;
import org.junit.*;
import org.junit.runner.*;

/**
 * @author shiyanlong
 * @date 2019/2/1
 **/
@RunWith(AndroidJUnit4.class)
public class DeviceManagerTest extends BaseAppLogTest {


    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(Manifest.permission.READ_PHONE_STATE);

    @Test
    public void load() {
        Context appContext = ApplicationProvider.getApplicationContext();
        final InitConfig config = new InitConfig("1234", "channel");
        config.clearDidAndIid("wsc");
        final ConfigManager cfgManger = new ConfigManager(appContext, config);
        cfgManger.getStatSp().edit().clear().apply();
        DeviceManager manager = new DeviceManager(appContext, cfgManger);

        HandlerThread ht = new HandlerThread("bd_tracker_w");
        ht.start();
        Handler mWorkHandler = new Handler(ht.getLooper(), new Callback() {
            @Override
            public boolean handleMessage(final Message msg) {
                Log.d("wscthread", "handleMessage: false");
                return false;
            }
        });
        manager.getProvider().setCacheHandler(mWorkHandler);

        if (config.isClearDidAndIid()) {
            manager.clearDidAndIid(config.getClearKey());
        }
        Assert.assertTrue(manager.load());
        Assert.assertTrue(manager.getHeader().length() > 10);
    }


}