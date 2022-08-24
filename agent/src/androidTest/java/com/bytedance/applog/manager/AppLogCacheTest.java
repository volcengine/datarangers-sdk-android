// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import android.support.test.runner.AndroidJUnit4;

import com.bytedance.applog.BaseAppLogTest;
import com.bytedance.applog.store.BaseData;
import com.bytedance.applog.store.EventV3;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

/**
 * Created by lixiao on 2020/8/20.
 */
@RunWith(AndroidJUnit4.class)
public class AppLogCacheTest extends BaseAppLogTest {

    @Test
    public void dumpData() throws Exception {
        AppLogCache cache = new AppLogCache();
        for (int i = 0; i < 400; i ++) {
            cache.cache(new EventV3("", true, ""));
        }
        int length = cache.dumpData(new ArrayList<BaseData>());
        Assert.assertEquals(length, 301);
    }

    @Test
    public void getArray() throws Exception {
        AppLogCache cache = new AppLogCache();
        for (int i = 0; i < 400; i ++) {
            cache.cache(new String[]{""});
        }
        int length = cache.getArray().length;
        Assert.assertEquals(length, 301);
    }
}
