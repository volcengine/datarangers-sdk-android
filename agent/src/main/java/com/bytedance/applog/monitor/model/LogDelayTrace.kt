// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.monitor.model

import org.json.JSONObject

/**
 * 日志延迟上报监控
 *
 * @author luodong.seu
 */
class LogDelayTrace : BaseTrace {

    var range: String? = null
    var count: Int = 0

    override fun loadParams(params: JSONObject) {
        params.put("range", range)
    }

    override fun category(): String {
        return "data_statistics"
    }

    override fun name(): String {
        return "log_delay"
    }

    override fun value(): Any {
        return count
    }
}