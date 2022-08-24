// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.monitor.model

import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter

/**
 * 异常监控模型
 *
 * @author luodong.seu
 */
class ExceptionTrace(private val type: String, private val throwable: Throwable) : BaseTrace {

    override fun loadParams(params: JSONObject) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        params.put("stack", sw.toString())
    }

    override fun category(): String {
        return "exception"
    }

    override fun name(): String {
        return type
    }

    override fun value(): Any {
        return throwable.message ?: ""
    }
}