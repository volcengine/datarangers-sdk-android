// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.network;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.bytedance.applog.AppLog;
import com.bytedance.applog.BaseAppLogTest;
import com.bytedance.applog.InitConfig;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.util.ReflectUtils;
import org.junit.*;
import org.junit.runner.*;

@RunWith(AndroidJUnit4.class)
public class CongestionControllerTest extends BaseAppLogTest {

    CongestionController senderDisasterRecovery;

    ConfigManager configManager;

    String prefix = "test_";

    @Before
    public void setUp() throws Exception {
        configManager = ReflectUtils.getFieldValue(AppLog.class, "sConfig");
        senderDisasterRecovery = new CongestionController(prefix, configManager);
    }

    @Test
    public void init() {
        setGradeSp(2, System.currentTimeMillis());
        senderDisasterRecovery.init();
        Assert.assertTrue(2 == (int) ReflectUtils.getFieldValue(senderDisasterRecovery, "mTableIndex"));

        // 模拟拥塞信息过期的情况
        setGradeSp(2, System.currentTimeMillis() - 24 * 60 * 60 * 1000L);
        senderDisasterRecovery.init();
        Assert.assertTrue(0 == (int) ReflectUtils.getFieldValue(senderDisasterRecovery, "mTableIndex"));
    }

    @Test
    public void isCanSend() {
        reset(0);

        configManager.getInitConfig().setCongestionControlEnable(false);
        for (int i = 0; i < 50; i++) {
            Assert.assertTrue(senderDisasterRecovery.isCanSend());
        }

        configManager.getInitConfig().setCongestionControlEnable(true);
        for (int i = 0; i < 50; i++) {
            if (!senderDisasterRecovery.isCanSend()) {
                Assert.assertTrue(i == 12);
                break;
            }
        }
    }

    @Test
    public void handleException() {
        reset(0);

        configManager.getInitConfig().setCongestionControlEnable(false);
        for (int i = 0; i < 20; i++) {
            senderDisasterRecovery.handleException();
        }
        Assert.assertTrue(0 == (int) ReflectUtils.getFieldValue(senderDisasterRecovery, "mTableIndex"));

        configManager.getInitConfig().setCongestionControlEnable(true);
        for (int i = 0; i < 20; i++) {
            senderDisasterRecovery.handleException();
        }
        Assert.assertTrue(CongestionController.TABLE_INTERVAL_COUNT.length - 1
                == (int) ReflectUtils.getFieldValue(senderDisasterRecovery, "mTableIndex"));
    }

    @Test
    public void handleSuccess() {
        reset(CongestionController.TABLE_INTERVAL_COUNT.length - 1);

        configManager.getInitConfig().setCongestionControlEnable(false);
        for (int i = 0; i < 20; i++) {
            senderDisasterRecovery.handleSuccess();
        }
        Assert.assertTrue(CongestionController.TABLE_INTERVAL_COUNT.length - 1
                == (int) ReflectUtils.getFieldValue(senderDisasterRecovery, "mTableIndex"));

        configManager.getInitConfig().setCongestionControlEnable(true);
        for (int i = 0; i < 15; i++) {
            if (i == 14) {
                // 快升到最高级
                Assert.assertTrue(1 == (int) ReflectUtils.getFieldValue(senderDisasterRecovery, "mTableIndex"));
            }
            senderDisasterRecovery.handleSuccess();
        }
        Assert.assertTrue(0 == (int) ReflectUtils.getFieldValue(senderDisasterRecovery, "mTableIndex"));
    }

    private void setGradeSp(int level, long time) {
        configManager.getStatSp().edit().putLong(prefix + "downgrade_time", time)
                .putInt(prefix + "downgrade_index", level).apply();
    }

    private void reset(int level) {
        ReflectUtils.setFieldValue(senderDisasterRecovery, "mTableIndex", level);
        ReflectUtils.setFieldValue(senderDisasterRecovery, "mHasSendCount", 0);
        ReflectUtils.setFieldValue(senderDisasterRecovery, "mContinueSuccSendCount", 0);
    }
}