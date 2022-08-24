// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.engine;

import android.support.test.runner.AndroidJUnit4;

import com.bytedance.applog.AppLog;
import com.bytedance.applog.BaseAppLogTest;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.manager.DeviceManager;
import com.bytedance.applog.util.ReflectUtils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author shiyanlong
 * @date 2019/2/2
 */
@RunWith(AndroidJUnit4.class)
public class ActivatorTest extends BaseAppLogTest {
    @Test
    public void doWork() {
        final ConfigManager configManager = ReflectUtils.getFieldValue(AppLog.class, "sConfig");
        final DeviceManager manager = ReflectUtils.getFieldValue(AppLog.class, "sDevice");
        configManager.getStatSp().edit().clear().apply();
        Assert.assertTrue(manager.load());

        Activator activator =
                new Activator((Engine) ReflectUtils.getFieldValue(Engine.class, "sInstance"));

        Assert.assertTrue(activator.doWork());

        Register reg = new Register((Engine) ReflectUtils.getFieldValue(Engine.class, "sInstance"));
        reg.checkToWork();

        Assert.assertTrue(activator.doWork());
    }

    @Test
    public void work() {
        final ConfigManager configManager = ReflectUtils.getFieldValue(AppLog.class, "sConfig");
        configManager.getStatSp().edit().clear().apply();

        final DeviceManager manager = ReflectUtils.getFieldValue(AppLog.class, "sDevice");
        Assert.assertTrue(manager.load());

        Activator activator =
                new Activator((Engine) ReflectUtils.getFieldValue(Engine.class, "sInstance"));
        long[] intervals = activator.getRetryIntervals();
        for (int i = 0; i < 12; ++i) {
            activator.setImmediately();
            long nextTime = activator.checkToWork() - System.currentTimeMillis();
            //            Assert.assertTrue(Math.abs(intervals[i % intervals.length] - nextTime) <
            // 5);
        }

        Register reg = new Register((Engine) ReflectUtils.getFieldValue(Engine.class, "sInstance"));
        reg.checkToWork();
        Assert.assertTrue(activator.doWork());
        Assert.assertTrue(activator.isStop());
    }
}
