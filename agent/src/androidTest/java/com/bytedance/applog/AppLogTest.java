// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.bytedance.applog.engine.Engine;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.manager.DeviceManager;
import com.bytedance.applog.profile.UserProfileCallback;
import com.bytedance.applog.util.ReflectUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AppLogTest {

    private static Engine engine;

    @BeforeClass
    public static void setUpAppLog() throws InterruptedException {
        Context appContext = ApplicationProvider.getApplicationContext();
        InitConfig config = new InitConfig("1234", "channel");
        config.setAutoStart(false);
        config.setHandleLifeCycle(false);
        AppLog.init(appContext, config);

        engine = ReflectUtils.getFieldValue(AppLog.class, "sEngine");

        HandlerThread ht = new HandlerThread("test");
        ht.start();
        Handler h = new Handler(ht.getLooper(), engine);
        ReflectUtils.setFieldValue(engine, "mNetHandler", h);

        DeviceManager dm = ReflectUtils.getFieldValue(AppLog.class, "sDevice");
        dm.load();
        ConfigManager cm = ReflectUtils.getFieldValue(AppLog.class, "sConfig");
        SharedPreferences sp = ReflectUtils.getFieldValue(cm, "mSp");
        sp.edit().putString("real_time_events", "[realtime_click,realtime_report]").apply();
        Thread.sleep(1000);
    }

    @AfterClass
    public static void tearDownAppLog() throws InterruptedException {
        ReflectUtils.execStaticMethod(AppLog.class, "destroy");
        Thread.sleep(1000);
    }

    @Test
    public void init() {
        boolean started = ReflectUtils.getFieldValue(engine, "mStarted");
        Assert.assertFalse(started);
    }

    @Test
    public void start() {
        AppLog.start();

        boolean started = ReflectUtils.getFieldValue(engine, "mStarted");
        Assert.assertTrue(started);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignore) {
        }

        Assert.assertEquals(AppLog.getAid(), "1234");
    }

    @Ignore
    @Test
    public void onEvent() {
        for (int i = 0; i < 10; i++) {
            AppLog.onEvent("event_v1");
        }
        UnitTestUtils.assertEventCount("event_v1", 10);
    }

    @Ignore
    @Test
    public void onEventV3() {
        for (int i = 0; i < 10; i++) {
            AppLog.onEventV3("TAG");
        }
        UnitTestUtils.assertEventV3Count("TAG", 10);

        AppLog.onEventV3("testLifeCycle");
        UnitTestUtils.assertEventV3Count("testLifeCycle", 1);
    }

    @Ignore
    @Test
    public void onMiscEvent() {
        for (int i = 0; i < 1; i++) {
            AppLog.onMiscEvent("log", new JSONObject());
        }
        UnitTestUtils.assertEventMiscCount("log", 0);
    }

    @Ignore
    @Test
    public void onMiscEventWithParam() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "test");
        } catch (JSONException ignore) {
        }
        for (int i = 0; i < 10; i++) {
            AppLog.onMiscEvent("log", jsonObject);
        }
        UnitTestUtils.assertEventMiscCount("log", 10);
    }

    @Ignore
    @Test
    public void onInternalEventV3() {
        for (int i = 0; i < 10; i++) {
            AppLog.onInternalEventV3("TAG", (JSONObject) null, "secondAppId", "JSONObject", "productType");
            AppLog.onInternalEventV3("TAG", (Bundle) null, "secondAppId", "Bundle", "productType");
        }
        UnitTestUtils.assertEventV3Count("second_app_TAG", 20);
    }

    @Ignore
    @Test
    public void onRealEvent() {
        AppLog.onEventV3("realtime_click");
        UnitTestUtils.assertEventV3Count("realtime_click", 0);
    }

    @Ignore
    @Test
    public void onRealEventLimit() {
        ReflectUtils.setFieldValue(engine, "mRealFailTs", System.currentTimeMillis());

        AppLog.onEventV3("realtime_report");
        UnitTestUtils.assertEventV3Count("realtime_report", 1);
    }

    @Ignore
    @Test
    public void onRealEventLimitClear() {
        ReflectUtils.setFieldValue(engine, "mRealFailTs", System.currentTimeMillis() - 16 * 60 * 1000);

        AppLog.onEventV3("realtime_report");
        UnitTestUtils.assertEventV3Count("realtime_report", 0);
    }

    @Test
    public void userProfile() {
        AppLog.userProfileSetOnce(new JSONObject(), new UserProfileCallback() {
            @Override
            public void onSuccess() {
                Assert.assertTrue(false);
            }

            @Override
            public void onFail(final int code) {
                Assert.assertTrue(true);
            }
        });
        AppLog.userProfileSync(new JSONObject(), new UserProfileCallback() {
            @Override
            public void onSuccess() {
                Assert.assertTrue(false);
            }

            @Override
            public void onFail(final int code) {
                Assert.assertTrue(true);
            }
        });
    }
}