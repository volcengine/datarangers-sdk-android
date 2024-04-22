// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.store;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.bytedance.applog.AppLog;
import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.BaseAppLogTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AccountCacheHelperTest extends BaseAppLogTest {
    AccountCacheHelper mAccountCacheHelper;

    @Before
    public void setUp() {
        Context appContext = ApplicationProvider.getApplicationContext();
        mAccountCacheHelper =
                new AccountCacheHelper((AppLogInstance) AppLog.getInstance(), appContext);
    }

    @Test
    public void cacheString() throws Exception {
        mAccountCacheHelper.cacheString("test", "test");
        String cache = mAccountCacheHelper.getCachedString("test");
        Assert.assertEquals(cache, "test");
    }

    @Test
    public void getCachedStringArray() throws Exception {
        String[] value = new String[]{"test"};
        mAccountCacheHelper.cacheStringArray("test", value);
        String[] result = mAccountCacheHelper.getCachedStringArray("test");
        Assert.assertArrayEquals(value, result);
    }

    @Test
    public void setAccount() throws Exception {
//        mAccountCacheHelper.cacheString("test", "test");
//        String cache = mAccountCacheHelper.getCachedString("test");
//        Assert.assertEquals(cache, "test");
//        mAccountCacheHelper.setAccount(new Account("test", "type"));
//        cache = mAccountCacheHelper.getCachedString("test");
//        Assert.assertEquals(cache, null);
    }

    @Test
    public void clear() throws Exception {
        mAccountCacheHelper.cacheString("test", "test");
        String cache = mAccountCacheHelper.getCachedString("test");
        Assert.assertEquals(cache, "test");
        mAccountCacheHelper.clear("test");
        cache = mAccountCacheHelper.getCachedString("test");
        Assert.assertEquals(cache, null);
    }
}
