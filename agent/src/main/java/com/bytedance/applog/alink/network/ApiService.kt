// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.alink.network

import android.net.Uri
import com.bytedance.applog.AppLogInstance
import com.bytedance.applog.alink.model.*
import org.json.JSONObject
import java.util.*

class ApiService(private var appLogInstance: AppLogInstance) {

    fun queryALinkData(
        uri: String,
        queryParam: ALinkQueryParam
    ): ApiResponse<ALinkData>? {
        return try {
            val responseStr: String = appLogInstance.netClient.get(
                appLogInstance.api.encryptUtils.encryptUrl(
                    addQueryParams(
                        uri,
                        queryParam.toJson()
                    )
                ),
                getHeaders()
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
    ): ApiResponse<AttributionData>? {
        return try {
            val responseStr: String? =
                appLogInstance.netClient.post(
                    appLogInstance.api.encryptUtils.encryptUrl(
                        addQueryParams(
                            uri,
                            queryParam.toJson()
                        )
                    ),
                    appLogInstance.api.encryptUtils.transformStrToByte(request.toString()),
                    getHeaders()
                )
            ApiResponse.parseJsonString(responseStr, AttributionData::class.java)
        } catch (t: Throwable) {
            null
        }
    }

    private fun getHeaders(): HashMap<String, String>? {
        val headers = HashMap<String, String>(2)
        return headers.apply {
            if (appLogInstance.encryptAndCompress)
                put("Content-Type", "application/octet-stream;tt-data=a")
            else {
                put("Content-Type", "application/json; charset=utf-8")
            }
        }
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