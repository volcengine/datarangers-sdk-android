// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.engine;

import android.support.test.runner.AndroidJUnit4;

import com.bytedance.applog.AppLog;
import com.bytedance.applog.BaseAppLogTest;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.manager.DeviceManager;
import com.bytedance.applog.util.ReflectUtils;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author shiyanlong
 * @date 2019/2/2
 */
@RunWith(AndroidJUnit4.class)
public class RegisterTest extends BaseAppLogTest {

    @Test
    public void dueTime() {
        final Engine engine = (Engine) ReflectUtils.getFieldValue(Engine.class, "sInstance");

        final Session session = engine.getSession();

        Register reg = new Register(engine);

        ReflectUtils.setFieldValue(session, "mHadUi", true);
        Assert.assertEquals(reg.nextInterval(), Register.REFRESH_UI);

        ReflectUtils.setFieldValue(session, "mHadUi", false);
        Assert.assertEquals(reg.nextInterval(), Register.REFRESH_BG);
    }

    @Test
    public void doWork() throws JSONException {
        final ConfigManager configManager = ReflectUtils.getFieldValue(AppLog.class, "sConfig");
        final DeviceManager manager = ReflectUtils.getFieldValue(AppLog.class, "sDevice");

        configManager.getStatSp().edit().clear().commit();
        Assert.assertTrue(manager.load());

        Register reg = new Register((Engine) ReflectUtils.getFieldValue(Engine.class, "sInstance"));
        Assert.assertTrue(reg.doWork());

        final String oldDid = AppLog.getDid();
        final String oldIid = AppLog.getIid();
        Assert.assertNotNull(oldDid);
        Assert.assertNotNull(oldIid);

        Assert.assertTrue(reg.doWork());

        Assert.assertEquals(oldDid, AppLog.getDid());
        Assert.assertEquals(oldIid, AppLog.getIid());
    }
}
