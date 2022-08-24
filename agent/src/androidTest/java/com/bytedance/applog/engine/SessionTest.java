// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.engine;

import android.support.test.runner.AndroidJUnit4;

import com.bytedance.applog.BaseAppLogTest;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author shiyanlong
 * @date 2019/2/14
 */
@RunWith(AndroidJUnit4.class)
public class SessionTest extends BaseAppLogTest {
    @Ignore
    @Test
    public void process() {
        //        final ConfigManager configManager = ReflectUtils.getFieldValue(AppLog.class,
        // "sConfig");
        //        final DeviceManager manager = ReflectUtils.getFieldValue(AppLog.class, "sDevice");
        //
        //        configManager.getStatSp().edit().clear().apply();
        //        manager.load();
        //
        //        // 直接从db里pack，再拿到pack
        //        final DbStore dbStore = UnitTestUtils.getDbStore();
        //
        //        ArrayList<Pack> packs = dbStore.queryPack(AppLog.getAid());
        //        while (packs.size() > 0) {
        //            dbStore.setResult(AppLog.getAid(), packs, new ArrayList<>(), new
        // ArrayList<>());
        //            packs = dbStore.queryPack(AppLog.getAid());
        //        }
        //
        //        // 以上为engine的准备工作
        //        int duration = 8;
        //        // 给 page1页面显示1秒，在1ms时候resume，在1001毫秒时候pause，共计10次
        //        for (int i = 0; i < duration; ++i) {
        //            // 给每个页面显示1秒钟
        //            //            long ts = (i + 1) * 3000;
        //            //            Page page = Navigator.resumePage("page" + i, "", ts, "", null);
        //            //            Navigator.pausePage(page, ts + 1000);
        //        }
        //
        //        // 第200000毫秒时候再启动，目的是刷新session，这样前一个session就结束了
        //        //        Navigator.resumePage("pageTrigger", "", 2000000, "", null);
        //        AppLog.flush();
        //
        //        dbStore.pack(AppLog.getAid(), new JSONObject());
        //        packs = dbStore.queryPack(AppLog.getAid());
        //        // 确认第一个pack里有terminate事件，且duration是10，上报单位是秒
        //        for (int i = 0; i < packs.size(); i++) {
        //            Pack pack = packs.get(i);
        //            String packString = new String(pack.data);
        //            Log.e("TeaLog", "sessiontest " + i + ", " + packString);
        //            if (i == 0) {
        //                Assert.assertTrue(packString.contains("terminate"));
        //                Assert.assertTrue(packString.contains("duration"));
        //            }
        //        }
        //        dbStore.setResult(AppLog.getAid(), packs, new ArrayList<Pack>(), new
        // ArrayList<Pack>());
    }
}
