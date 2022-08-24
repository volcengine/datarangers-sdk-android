// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.alink.model

import org.json.JSONObject

/**
 * 很Base的Data，定义和Json转换的接口
 * @author chenguanzhong
 * @date 2021/1/27
 **/
abstract class BaseData {


    /**
     * 转Json对象
     */
    abstract fun toJson(): JSONObject

    /**
     * 使用Json初始化
     */
    abstract fun initWithJson(json: JSONObject?)

    /**
     * 转map
     */
    fun toMap(): Map<String, String?> {
        return HashMap<String, String?>().also { map ->
            toJson().run {
                for (key in keys()) {
                    map[key] = optString(key, null)
                }
            }
        }
    }

    companion object {
        fun <T : BaseData> fromJson(json: JSONObject?, clazz: Class<T>): T? {
            return json?.run {
                val data = clazz.getConstructor().newInstance()
                data.initWithJson(this)
                data
            }
        }
    }

    override fun toString(): String {
        return toJson().toString()
    }
}