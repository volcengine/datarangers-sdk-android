// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.oneid

import android.os.Handler
import android.os.Looper
import com.bytedance.applog.concurrent.TTExecutors
import com.bytedance.applog.engine.Engine
import com.bytedance.applog.log.LogInfo
import com.bytedance.applog.server.Api
import com.bytedance.applog.util.Utils
import org.json.JSONObject

/**
 * OneID 服务管理类
 * 负责多口径 ID 绑定
 * @author: baoyongzhang
 * @date: 2022/10/9
 */
class OneIDManager(val engine: Engine) {

    private val appLogInstance = engine.appLog
    private val mainHandler = Handler(Looper.getMainLooper())
    private val logger = engine.appLog.logger

    /**
     * 绑定多口径 ID
     */
    fun bind(identities: Map<String, String>, callback: IDBindCallback?) {
        TTExecutors.getNormalExecutor().submit {
            val header = JSONObject()
            Utils.copy(header, engine.dm.header)

            logger.debug(LogInfo.Category.ONE_ID, "BindID identities: {}", identities)

            header.put("identities", identities.toJSONObject())
            val request = Api.buildRequestBody(header)
            val idBindUri = engine.uriConfig.idBindUri
            val response = idBindUri?.run {
                appLogInstance.api.bindID(this, request)
            }
            if (response == null) {
                val msg = "BindID http request error, url=$idBindUri"
                logger.warn(LogInfo.Category.ONE_ID, msg)
                callback?.reportFail(-2, msg)
                return@submit
            }
            val code = response.optInt("status_code")
            if (code != 200) {
                val message = response.optString("status_message")
                logger.warn(LogInfo.Category.ONE_ID, message)
                callback?.reportFail(code, message)
                return@submit
            }
            val data = response.optJSONObject("data")
            val result = IDBindResult(
                data?.optString("ssid"),
                data?.optString("failed_id_list")
            )
            logger.debug(LogInfo.Category.ONE_ID, "BindID reportSuccess, result: {}", result)
            callback?.reportSuccess(result)
        }
    }

    private fun IDBindCallback.reportSuccess(result: IDBindResult) {
        mainHandler.post {
            this.onSuccess(result)
        }
    }

    private fun IDBindCallback.reportFail(code: Int, message: String?) {
        mainHandler.post {
            this.onFail(code, message)
        }
    }

}

private fun <V> Map<String, V>.toJSONObject(): JSONObject {
    val jsonObject = JSONObject()
    this.forEach {
        jsonObject.put(it.key, it.value)
    }
    return jsonObject
}
