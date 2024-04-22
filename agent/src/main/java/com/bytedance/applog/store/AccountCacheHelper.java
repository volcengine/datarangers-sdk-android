// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.store;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

import com.bytedance.applog.AppLogInstance;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * cache value to android system account
 *
 * <p>Created by qianhong on 2017/5/5.
 */
public class AccountCacheHelper extends CacheHelper {
    private final AccountManager mAccountManager;
    private Account mAccount;
    private final ConcurrentHashMap<String, String> mCache = new ConcurrentHashMap<>();
    private final AppLogInstance appLogInstance;

    public AccountCacheHelper(AppLogInstance appLogInstance, Context context) {
        this.appLogInstance = appLogInstance;
        mAccountManager = AccountManager.get(context);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void cacheString(String key, String value) {
        if (mAccount == null) {
            mCache.put(key, value);
            return;
        }
        if (key == null || value == null) {
            return;
        }
        try {
            mAccountManager.setUserData(mAccount, key, value);
        } catch (Throwable e) {
            appLogInstance
                    .getLogger()
                    .error(
                            Collections.singletonList("AccountCacheHelper"),
                            "Set user data failed",
                            e);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public String getCachedString(String key) {
        if (mAccount == null) {
            return mCache.get(key);
        }
        try {
            return mAccountManager.getUserData(mAccount, key);
        } catch (Throwable e) {
            appLogInstance
                    .getLogger()
                    .error(
                            Collections.singletonList("AccountCacheHelper"),
                            "Get user data failed",
                            e);
        }
        return null;
    }

    @Override
    protected String[] getCachedStringArray(String key) {
        String val = getCachedString(key);
        if (TextUtils.isEmpty(val)) {
            return null;
        }
        return val.split("\n");
    }

    @Override
    protected void cacheStringArray(String cachedKey, String[] value) {
        if (cachedKey == null || value == null) {
            return;
        }
        String storedVal = TextUtils.join("\n", value);
        cacheString(cachedKey, storedVal);
    }

    @SuppressLint("MissingPermission")
    public void setAccount(final Account account) {
        if (account != null) {
            this.mAccount = account;
            if (mCache.size() <= 0) {
                return;
            }
            mHandler.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (mCache.size() <= 0 || mAccountManager == null) {
                                    return;
                                }
                                for (Map.Entry<String, String> entry : mCache.entrySet()) {
                                    if (entry == null) continue;
                                    mAccountManager.setUserData(
                                            account, entry.getKey(), entry.getValue());
                                }
                                mCache.clear();
                            } catch (Throwable e) {
                                appLogInstance
                                        .getLogger()
                                        .error(
                                                Collections.singletonList("AccountCacheHelper"),
                                                "Set account failed",
                                                e);
                            }
                        }
                    });
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void clear(String key) {
        mCache.remove(key);
        try {
            if (mAccount != null && mAccountManager != null) {
                mAccountManager.setUserData(mAccount, key, null);
            }
        } catch (Throwable ignored) {
        }
        super.clear(key);
    }
}
