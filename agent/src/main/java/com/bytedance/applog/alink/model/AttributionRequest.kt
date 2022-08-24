// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.alink.model

import org.json.JSONObject

/**
 * 获取归因数据的请求body
 * @author chenguanzhong
 * @date 2021/1/28
 **/
class AttributionRequest : BaseData() {

    var aid: String? = null
    var deviceID: String? = null
    var bdDid: String? = null
    var installId: String? = null
    var os: String? = null
    var caid: String? = null
    var isNewUser: Boolean = false
    var existAppCache: Boolean = false
    var appVersion: String? = null
    var channel: String? = null
    var androidId: String? = null
    var imei: String? = null
    var oaid: String? = null
    var googleAid: String? = null
    var ip: String? = null
    var ua: String? = null
    var deviceModel: String? = null
    var osVersion: String? = null


    override fun toJson(): JSONObject {
        return JSONObject().apply {
            put("aid", aid)
            //deviceId和bdId至少一个不为空
            put("device_id", deviceID)
            put("bd_did", bdDid)
            put("install_id", installId)
            put("os", os)
            put("caid", caid)
            put("androidid", androidId)
            put("imei", imei)
            put("oaid", oaid)
            put("google_aid", googleAid)
            put("ip", ip)
            put("ua", ua)
            put("device_model", deviceModel)
            put("os_version", osVersion)
            put("is_new_user", isNewUser)
            put("exist_app_cache", existAppCache)
            put("app_version", appVersion)
            put("channel", channel)
        }
    }

    override fun initWithJson(json: JSONObject?) {
        //do nothing
    }
}