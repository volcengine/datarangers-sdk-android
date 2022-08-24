// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.alink.model

import org.json.JSONException
import org.json.JSONObject


/**
 * 标准Api返回
 * @author chenguanzhong
 * @date 2021/1/28
 **/
class ApiResponse<T : BaseData> {
    var code = 0
    var message: String? = null
    var data: T? = null

    companion object {
        fun <T : BaseData> parseJsonString(string: String?, clazz: Class<T>): ApiResponse<T> {
            val response = JSONObject(string)
            return ApiResponse<T>().apply {
                code = response.optInt("code")
                message = response.optString("message")
                data = BaseData.fromJson(response.optJSONObject("data"), clazz)
            }
        }
    }
}