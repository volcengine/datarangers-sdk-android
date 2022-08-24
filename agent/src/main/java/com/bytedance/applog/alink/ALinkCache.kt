// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.alink

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.bytedance.applog.alink.model.BaseData
import com.bytedance.applog.store.SharedPreferenceCacheHelper
import org.json.JSONObject

private const val EXPIRE_TS_KEY = "expire_ts"
private const val DATA_KEY = "data"

/**
 * 缓存管理器
 * @author chenguanzhong
 * @date 2021/1/27
 **/
class ALinkCache(applicationContext: Application, spName: String) {

    companion object {
        const val NO_EXPIRE = -1L
        const val SP_NAME = "ALINK_CACHE_SP"
    }

    private var mSharedPreference: SharedPreferences? = null

    init {
        mSharedPreference = SharedPreferenceCacheHelper.getSafeSharedPreferences(
            applicationContext,
            spName,
            Context.MODE_PRIVATE
        )
    }

    /**
     * 按key获取cache对象
     * @param key
     * @param clazz 对象类型
     * @return 对象数据，若未命中或已过期返回null
     */
    fun <T : BaseData> getData(key: String, clazz: Class<T>): T? {
        return try {
            mSharedPreference?.getString(key, null)?.run {
                val json = JSONObject(this)
                val expireTs = json.optLong(EXPIRE_TS_KEY)
                val valueAvailable = expireTs == NO_EXPIRE ||
                        (expireTs > 0 && System.currentTimeMillis() < expireTs)
                if (valueAvailable)
                    BaseData.fromJson(json, clazz)
                else {
                    mSharedPreference?.edit()?.remove(key)?.apply()
                    null
                }
            }
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * 将data存入缓存
     * @param key 键
     * @param data 数据
     * @param expireTs 过期时间，负数表示永久
     */
    fun putData(key: String, data: BaseData, expireTs: Long) {
        val json = data.toJson()
        if (expireTs == NO_EXPIRE) {
            json.put(EXPIRE_TS_KEY, NO_EXPIRE)
        } else {
            json.put(EXPIRE_TS_KEY, System.currentTimeMillis() + expireTs)
        }
        mSharedPreference?.edit()?.putString(key, json.toString())?.apply()
    }

    fun putString(key: String, value: String?, expireTs: Long) {
        val json = JSONObject()
        json.put(DATA_KEY, value)
        if (expireTs == NO_EXPIRE) {
            json.put(EXPIRE_TS_KEY, NO_EXPIRE)
        } else {
            json.put(EXPIRE_TS_KEY, System.currentTimeMillis() + expireTs)
        }
        mSharedPreference?.edit()?.putString(key, json.toString())?.apply()
    }

    fun getString(key: String): String? {
        return try {
            mSharedPreference?.getString(key, null)?.run {
                val json = JSONObject(this)
                val expireTs = json.optLong(EXPIRE_TS_KEY)
                val valueAvailable = expireTs == NO_EXPIRE ||
                        (expireTs > 0 && System.currentTimeMillis() < expireTs)
                if (valueAvailable) json.optString(
                    DATA_KEY
                ) else {
                    mSharedPreference?.edit()?.remove(key)?.apply()
                    null
                }
            }
        } catch (t: Throwable) {
            null
        }
    }
}