// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.alink.network

import android.net.Uri
import com.bytedance.applog.AppLogInstance
import com.bytedance.applog.alink.model.ALinkData
import com.bytedance.applog.alink.model.ALinkQueryParam
import com.bytedance.applog.alink.model.ApiResponse
import com.bytedance.applog.alink.model.AttributionData
import com.bytedance.applog.alink.model.AttributionRequest
import com.bytedance.applog.network.INetworkClient
import com.bytedance.applog.server.Api
import com.bytedance.applog.util.EncryptUtils
import org.json.JSONObject

class ApiService(private var appLogInstance: AppLogInstance) {

    fun queryALinkData(
        uri: String,
        queryParam: ALinkQueryParam
    ): ApiResponse<ALinkData>? {
        return try {
            val responseStr = String(
                appLogInstance.netClient.execute(
                    INetworkClient.METHOD_GET,
                    appLogInstance.api.encryptUtils.encryptUrl(
                        addQueryParams(
                            uri,
                            queryParam.toJson()
                        )
                    ),
                    null,
                    getHeaders(),
                    INetworkClient.RESPONSE_TYPE_STRING,
                    true,
                    Api.HTTP_DEFAULT_TIMEOUT
                )
            )
            ApiResponse.parseJsonString(responseStr, ALinkData::class.java)
        } catch (t: Throwable) {
            null
        }
    }

    fun queryAttributionData(
        uri: String,
        request: AttributionRequest,
        queryParam: ALinkQueryParam
    ): ApiResponse<AttributionData> {
        return try {
            val responseStr =
                String(
                    appLogInstance.netClient.execute(
                        INetworkClient.METHOD_POST,
                        appLogInstance.api.encryptUtils.encryptUrl(
                            addQueryParams(
                                uri,
                                queryParam.toJson()
                            )
                        ),
                        request.toJson(),
                        getHeaders(),
                        INetworkClient.RESPONSE_TYPE_STRING,
                        true,
                        Api.HTTP_DEFAULT_TIMEOUT
                    )
                )
            ApiResponse.parseJsonString(responseStr, AttributionData::class.java)
        } catch (t: Throwable) {
            ApiResponse.parseThrowable(t)
        }
    }

    private fun getHeaders(): HashMap<String, String>? {
        val headers = HashMap<String, String>(2)
        val initConfig = appLogInstance.initConfig
        if (null != initConfig) {
            val httpHeaders = initConfig.httpHeaders
            if (null != httpHeaders && httpHeaders.isNotEmpty()) {
                headers.putAll(httpHeaders)
            }
        }
        return EncryptUtils.putContentTypeHeader(headers, appLogInstance)
    }

    private fun addQueryParams(url: String, params: JSONObject): String? {
        val uri = Uri.parse(url)
        val builder = uri.buildUpon()
        val it = params.keys()
        while (it.hasNext()) {
            val key = it.next()
            val value = params.optString(key)
            if (!value.isNullOrEmpty()) {
                builder.appendQueryParameter(key, params.optString(key))
            }
        }
        return builder.build().toString()
    }
}