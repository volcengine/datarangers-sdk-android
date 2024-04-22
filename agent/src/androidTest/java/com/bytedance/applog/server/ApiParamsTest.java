// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.server;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.bytedance.applog.AppLog;
import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.BaseAppLogTest;
import com.bytedance.applog.Level;
import com.bytedance.applog.engine.Engine;
import com.bytedance.applog.util.ReflectUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

/** Created by lixiao on 2020/8/20. */
@RunWith(AndroidJUnit4.class)
public class ApiParamsTest extends BaseAppLogTest {

    @Test
    public void appendNetParams() throws JSONException {
        Context appContext = ApplicationProvider.getApplicationContext();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("test", "test");
        jsonObject.put(Api.KEY_VERSION_CODE, 1);
        jsonObject.put(Api.KEY_MANIFEST_VERSION_CODE, 1);
        jsonObject.put(Api.KEY_UPDATE_VERSION_CODE, 1);
        String result =
                ((AppLogInstance) AppLog.getInstance())
                        .getApiParamsUtil()
                        .appendNetParams(jsonObject, "url", true, Level.L0);
        Assert.assertTrue(result.contains("_rticket="));
        Assert.assertTrue(result.contains("&ac=wifi"));
        Assert.assertTrue(result.contains("device_platform=android"));
        Assert.assertTrue(result.contains("ssmix=a"));
    }

    @Test
    public void appendParamsToMap() throws Exception {
        Context appContext = ApplicationProvider.getApplicationContext();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("test", "test");
        jsonObject.put(Api.KEY_VERSION_CODE, 1);
        jsonObject.put(Api.KEY_MANIFEST_VERSION_CODE, 1);
        jsonObject.put(Api.KEY_UPDATE_VERSION_CODE, 1);
        HashMap<String, String> hashMap = new HashMap<String, String>();
        ((AppLogInstance) AppLog.getInstance())
                .getApiParamsUtil()
                .appendParamsToMap(jsonObject, true, hashMap, Level.L0);
        Assert.assertTrue(hashMap.containsKey("_rticket"));
        Assert.assertTrue(hashMap.containsKey("ac"));
        Assert.assertTrue(hashMap.containsKey("ssmix"));
        Assert.assertTrue(hashMap.containsKey("os_version"));
        Assert.assertTrue(hashMap.containsKey("device_platform"));
    }

    @Test
    public void getSendLogUris() throws Exception {
        Engine engine = ReflectUtils.getFieldValue(AppLog.class, "sEngine");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("test", "test");
        jsonObject.put(Api.KEY_VERSION_CODE, 1);
        jsonObject.put(Api.KEY_MANIFEST_VERSION_CODE, 1);
        jsonObject.put(Api.KEY_UPDATE_VERSION_CODE, 1);
        HashMap<String, String> hashMap = new HashMap<String, String>();
        String[] result =
                ((AppLogInstance) AppLog.getInstance())
                        .getApiParamsUtil()
                        .getSendLogUris(engine, jsonObject, AppLogInstance.DEFAULT_EVENT);
        Assert.assertTrue(result[0].contains("https://log.snssdk.com/service/2/app_log/"));
        Assert.assertTrue(result[1].contains("https://applog.snssdk.com/service/2/app_log/"));
    }

    @Test
    public void getValues() throws Exception {
        Engine engine = ReflectUtils.getFieldValue(AppLog.class, "sEngine");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("test", "test");
        jsonObject.put(Api.KEY_VERSION_CODE, 1);
        jsonObject.put(Api.KEY_MANIFEST_VERSION_CODE, 1);
        jsonObject.put(Api.KEY_UPDATE_VERSION_CODE, 1);
        Integer result =
                ((AppLogInstance) AppLog.getInstance())
                        .getApiParamsUtil()
                        .getValue(jsonObject, Api.KEY_VERSION_CODE, 0, Integer.class);
        Assert.assertEquals(result.intValue(), 1);
    }
}
