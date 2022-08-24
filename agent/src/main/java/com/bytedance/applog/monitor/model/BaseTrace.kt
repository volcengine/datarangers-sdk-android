// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.monitor.model

import com.bytedance.applog.util.TLog
import org.json.JSONObject

/**
 * 监控模型的基类
 *
 * @author luodong.seu
 */
interface BaseTrace {

    companion object {
        const val CATEGORY_KEY = "metrics_category"
        const val NAME_KEY = "metrics_name"
        const val VALUE_KEY = "metrics_value"
    }

    /**
     * Trace的参数
     */
    fun getTraceParams(): JSONObject {
        val json = JSONObject()
        try {
            json.put(CATEGORY_KEY, category())
            json.put(NAME_KEY, name())
            json.put(VALUE_KEY, value())
            loadParams(json)
        } catch (e: Throwable) {
            TLog.e(e)
        }
        return json;
    }

    /**
     * 加载自定义参数
     */
    fun loadParams(params: JSONObject)

    /**
     * 分类
     */
    fun category(): String

    /**
     * 监控名称
     */
    fun name(): String

    /**
     * 监控值
     */
    fun value(): Any
}