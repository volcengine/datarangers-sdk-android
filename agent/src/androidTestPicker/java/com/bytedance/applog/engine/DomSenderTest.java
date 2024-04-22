// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.engine;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bytedance.applog.AppLog;
import com.bytedance.applog.InitConfig;
import com.bytedance.applog.util.ReflectUtils;
import org.junit.*;
import org.junit.runner.*;

@RunWith(AndroidJUnit4.class)
public class DomSenderTest {

    @Before
    public void setUp() {
        Context appContext = ApplicationProvider.getApplicationContext();
        InitConfig config = new InitConfig("1234", "channel");
        AppLog.init(appContext, config);
    }

    @After
    public void tearDown() {
        ReflectUtils.execStaticMethod(AppLog.class, "destroy");
    }

    @Test
    public void startSimulator() {
        Engine engine = ReflectUtils.getFieldValue(Engine.class, "sInstance");
        try {
            engine.startSimulator("cookie");
        } catch (Exception e) {
        }
        Assert.assertNotNull(ReflectUtils.getFieldValue(engine, "mDomSender"));

    }
}