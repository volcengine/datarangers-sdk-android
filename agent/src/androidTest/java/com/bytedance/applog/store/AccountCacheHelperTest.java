// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.store;

import android.accounts.Account;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.bytedance.applog.BaseAppLogTest;
import org.junit.*;
import org.junit.runner.*;

/**
 * Created by lixiao on 2020/8/20.
 */

@RunWith(AndroidJUnit4.class)
public class AccountCacheHelperTest extends BaseAppLogTest {
    AccountCacheHelper mAccountCacheHelper;
    @Before
    public void setUp() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        mAccountCacheHelper = new AccountCacheHelper(appContext);
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
