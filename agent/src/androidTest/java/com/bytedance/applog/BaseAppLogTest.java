// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.bytedance.applog.util.ReflectUtils;

import org.junit.*;
import org.junit.runner.*;

/**
 * There's no need to copy & paste setup & teardown code on every test class, just create a BaseTest!
 * use @Before will be called on every test, which may lead a crash.
 * Created by lixiao on 2020/8/26.
 */
@RunWith(AndroidJUnit4.class)
abstract public class BaseAppLogTest {
    @BeforeClass
    public static void setUpAppLog() throws InterruptedException {
        Context appContext = InstrumentationRegistry.getTargetContext();
        InitConfig config = new InitConfig("1234", "channel");
        config.setUriConfig(UriConstants.DEFAULT);
        AppLog.init(appContext, config);
        // there's a bug without sleep!!!
        Thread.sleep(1000);
    }

    @AfterClass
    public static void tearDownAppLog() {
        ReflectUtils.execStaticMethod(AppLog.class, "destroy");
    }
}
