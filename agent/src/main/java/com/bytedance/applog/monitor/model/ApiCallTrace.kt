// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.monitor.model

import org.json.JSONObject

/**
 * API调用次数统计
 *
 * @author luodong.seu
 */
class ApiCallTrace() : BaseTrace {

    companion object {
        private const val NAME = "api_calls"
    }

    var apiName: String? = null
    var time: Long = 0
    private var count: Int = 1

    override fun loadParams(params: JSONObject) {
        params.put("api_name", apiName)
        params.put("api_time", time)
    }

    override fun category(): String {
        return "data_statistics"
    }

    override fun name(): String {
        return NAME
    }

    override fun value(): Any {
        return count
    }
}