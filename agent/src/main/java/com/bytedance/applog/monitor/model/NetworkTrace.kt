// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.monitor.model

import org.json.JSONObject

/**
 * 网络监控模型
 *
 * @author luodong.seu
 */
class NetworkTrace : BaseTrace {

    var errorCode: Int? = null
    var errorMsg: String? = null
    var url: String? = null
    var duration: Long = 0

    override fun loadParams(params: JSONObject) {
        params.put("err_code", errorCode)
        params.put("err_message", errorMsg)
    }

    override fun category(): String {
        return "network_service"
    }

    override fun name(): String {
        return url?.let { url?.substring(0, it.indexOf("?")) } ?: ""
    }

    override fun value(): Any {
        return duration
    }

}