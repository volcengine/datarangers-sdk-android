// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.text.TextUtils;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.manager.DeviceManager;
import com.bytedance.applog.util.ReflectUtils;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.*;

@RunWith(AndroidJUnit4.class)
public class AbTest {

    private ConfigManager cm;

    private DeviceManager dm;
    @BeforeClass
    public static void setUpAppLog() throws InterruptedException {
        Context appContext = ApplicationProvider.getApplicationContext();
        InitConfig config = new InitConfig("1234", "channel");
        config.setHandleLifeCycle(false);
        AppLog.init(appContext, config);
        Thread.sleep(1000);
    }

    @AfterClass
    public static void tearDownAppLog() throws InterruptedException {
        Thread.sleep(3000);
        ReflectUtils.execStaticMethod(AppLog.class, "destroy");
    }

    @Before
    public void setUp() {
        cm = ReflectUtils.getFieldValue(AppLog.class, "sConfig");
        cm.getStatSp().edit().clear().apply();
        dm = ReflectUtils.getFieldValue(AppLog.class, "sDevice");
    }

    @Test
    public void getSetAb() {
        dm.setAbConfig(null);
        AppLog.setExternalAbVersion(null);
        Assert.assertEquals(AppLog.getAbSdkVersion(), "");
        AppLog.setExternalAbVersion("This is ab sdk");
        Assert.assertEquals(AppLog.getAbSdkVersion(), "This is ab sdk");
        AppLog.setExternalAbVersion(null);
    }

    @Ignore
    @Test
    public void testEventWithAb() {
        dm.setAbConfig(null);
        AppLog.setExternalAbVersion("This is ab sdk");
        for (int i = 0; i < 10; i++) {
            AppLog.onEventV3("TAG");
        }
        UnitTestUtils.assertCount("ab_sdk_version", AppLog.getAbSdkVersion(), 10 + 1);//one for launch.
        AppLog.setExternalAbVersion(null);
    }

    @Test
    public void testEventWithoutAb() {
        dm.setAbConfig(null);
        AppLog.setExternalAbVersion(null);
        for (int i = 0; i < 10; i++) {
            AppLog.onEventV3("TAG");
        }
        UnitTestUtils.assertCount("ab_sdk_version", AppLog.getAbSdkVersion(), 0);
    }

    @Ignore
    @Test
    public void setGetAbConfig() {
        dm.setAbConfig(null);
        JSONObject abConfig = new JSONObject();
        try {
            JSONObject key1 = new JSONObject();
            key1.put("val", "val1");
            key1.put("vid", "vid1");
            abConfig.put("key1", key1);

            JSONObject key2 = new JSONObject();
            key2.put("val", "val2");
            key2.put("vid", "vid2");
            abConfig.put("key2", key2);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        dm.setAbConfig(abConfig);

        Assert.assertEquals(AppLog.getAbConfig("key1", ""), "val1");
        Assert.assertEquals(AppLog.getAbSdkVersion(), "vid1");

        Assert.assertEquals(AppLog.getAbConfig("key1", ""), "val1");
        Assert.assertEquals(AppLog.getAbSdkVersion(), "vid1");

        Assert.assertEquals(AppLog.getAbConfig("key2", ""), "val2");
        Assert.assertEquals(AppLog.getAbSdkVersion(), "vid1,vid2");

        Assert.assertEquals(AppLog.getAbConfig("key2", ""), "val2");
        Assert.assertEquals(AppLog.getAbSdkVersion(), "vid1,vid2");
    }

    @Ignore
    @Test
    public void updateExposedVidByNewConfig() {
        setGetAbConfig();

        dm.setExternalAbVersion("vid3");
        assertVidSetEquals("vid1,vid2,vid3", AppLog.getAbSdkVersion());

        JSONObject abConfig = new JSONObject();
        JSONObject test1 = new JSONObject();
        try {
            test1.put("val", "val2_plus");
            test1.put("vid", "vid2");
            abConfig.put("key2", test1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        dm.setAbConfig(abConfig);

        Assert.assertEquals(AppLog.getAbConfig("key2", ""), "val2_plus");
        assertVidSetEquals("vid2,vid3", AppLog.getAbSdkVersion());

        dm.setExternalAbVersion("vid4");
        assertVidSetEquals("vid2,vid4", AppLog.getAbSdkVersion());
    }

    private void assertVidSetEquals(String expectedVidList, String actualVidList) {
        Set<String> expectedVidSet = new HashSet<>();
        if (!TextUtils.isEmpty(expectedVidList)) {
            String[] expectedVids = expectedVidList.split(",");
            for (String vid : expectedVids) {
                if (!TextUtils.isEmpty(vid)) {
                    expectedVidSet.add(vid);
                }
            }
        }
        Set<String> actualVidSet = new HashSet<>();
        if (!TextUtils.isEmpty(actualVidList)) {
            String[] actualVids = actualVidList.split(",");
            for (String vid : actualVids) {
                if (!TextUtils.isEmpty(vid)) {
                    actualVidSet.add(vid);
                }
            }
        }
        Assert.assertEquals(expectedVidSet, actualVidSet);
    }
}