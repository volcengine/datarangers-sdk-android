// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.store;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;

import com.bytedance.applog.log.LoggerImpl;
import com.bytedance.applog.server.Api;

import java.util.Collections;

/**
 * 保存到sp里面
 *
 * <p>Created by qianhong on 2017/5/5.
 */
public class SharedPreferenceCacheHelper extends CacheHelper {
    private final SharedPreferences mSp;
    private SharedPreferences mDeviceIdSp;

    public static final String GLOBAL_CACHE_HELPER_SP_NAME = "_global_cache";

    private static SharedPreferenceCacheHelper globalCacheHelper;

    /** 遇到null的时候保存为"" */
    private boolean saveNullAsEmpty = false;

    public static synchronized SharedPreferenceCacheHelper getGlobal(Context context) {
        if (null == globalCacheHelper) {
            globalCacheHelper =
                    new SharedPreferenceCacheHelper(context, GLOBAL_CACHE_HELPER_SP_NAME, true);
        }
        return globalCacheHelper;
    }

    public SharedPreferenceCacheHelper(
            Context context, String cacheFileName, boolean saveNullAsEmpty) {
        mSp =
                SharedPreferenceCacheHelper.getSafeSharedPreferences(
                        context, cacheFileName, Context.MODE_PRIVATE);
        this.saveNullAsEmpty = saveNullAsEmpty;
    }

    public SharedPreferenceCacheHelper(Context context, String cacheFileName, String deviceSpName) {
        mSp =
                SharedPreferenceCacheHelper.getSafeSharedPreferences(
                        context, cacheFileName, Context.MODE_PRIVATE);
        mDeviceIdSp =
                SharedPreferenceCacheHelper.getSafeSharedPreferences(
                        context, deviceSpName, Context.MODE_PRIVATE);
    }

    /**
     * 如果存在则返回缓存，否则loader加载缓存内容并存储
     *
     * @param key key
     * @param loader CacheLoader
     * @return 缓存的内容
     */
    public synchronized String getOrLoad(String key, CacheLoader loader) throws Throwable {
        if (hasKey(key)) {
            return getCachedString(key);
        }
        String cache = null;
        if (null != loader) {
            cache = loader.load();
        }
        cacheString(key, cache);
        return cache;
    }

    @Override
    public void cacheString(String key, String value) {
        storeValue(key, value);
    }

    @Override
    public String getCachedString(String key) {
        return getValue(key);
    }

    @Override
    public String[] getCachedStringArray(String key) {
        String storedVal = getValue(key);
        if (TextUtils.isEmpty(storedVal)) return null;
        return storedVal.split("\n");
    }

    @Override
    public void cacheStringArray(String cachedKey, String[] value) {
        if (cachedKey == null || value == null) return;
        String storedValue = TextUtils.join("\n", value);
        storeValue(cachedKey, storedValue);
    }

    public String getValue(String key) {
        return getSp(key).getString(key, null);
    }

    public boolean hasKey(String key) {
        return getSp(key).contains(key);
    }

    public void storeValue(String key, String value) {
        if (!saveNullAsEmpty && TextUtils.isEmpty(value)) return;
        SharedPreferences.Editor editor = getSp(key).edit();
        if (saveNullAsEmpty && null == value) {
            editor.putString(key, "");
        } else {
            editor.putString(key, value);
        }
        editor.apply();
    }

    public SharedPreferences getSp(String key) {
        return Api.KEY_DEVICE_ID.equals(key) && null != mDeviceIdSp ? mDeviceIdSp : mSp;
    }

    @Override
    public void clear(String key) {
        SharedPreferences sp = getSp(key);
        if (sp != null && sp.contains(key)) {
            SharedPreferences.Editor editor = getSp(key).edit();
            editor.remove(key).apply();
        }
        super.clear(key);
    }

    public interface CacheLoader {
        String load() throws Throwable;
    }

    /**
     * 获取shared preference实例
     *
     * @param app Context
     * @param name sp名
     * @param mode @PreferencesMode
     * @return SharedPreferences
     */
    public static SharedPreferences getSafeSharedPreferences(
            final Context app, final String name, final int mode) {
        // when build version >= O(26), getSharedPreferences from application context will throw
        // SharedPreferences in credential encrypted storage are not available
        Context spContext = app;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                spContext = app.createDeviceProtectedStorageContext();
                if (!spContext.moveSharedPreferencesFrom(app, name)) {
                    LoggerImpl.global()
                            .warn(
                                    Collections.singletonList("SharedPreferenceCacheHelper"),
                                    "Failed to migrate " + "shared preferences.");
                }
            } catch (Throwable e) {
                LoggerImpl.global()
                        .error(
                                Collections.singletonList("SharedPreferenceCacheHelper"),
                                "Create protected storage context failed",
                                e);
            }
        }
        return spContext.getSharedPreferences(name, mode);
    }
}
