// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.alink.model

import org.json.JSONObject

/**
 * 链接参数
 * @author zhengpeijian
 * @date 2021/6/29
 */
class ALinkQueryParam : BaseData() {
    //必选参数
    var aid: String? = null
    var bdDid: String? = null
    var ssid: String? = null
    var userUniqueId: String? = null
    var abVersion: String? = null
    var webSsid: String? = null

    // 新增参数
    var androidId: String? = null
    var imei: String? = null
    var googleAid: String? = null
    var os: String? = null
    var osVersion: String? = null
    var deviceModel: String? = null

    //获取归因数据时不是必选
    var token: String? = null

    //可选
    var clickTime: Int? = null
    var trShareUser: String? = null
    var trAdMaster: String? = null
    var trParam1: String? = null
    var trParam2: String? = null
    var trParam3: String? = null
    var trParam4: String? = null


    override fun toJson(): JSONObject {
        return JSONObject().apply {
            put("token", token)
            put("aid", aid)
            put("os", os)
            put("bd_did", bdDid)
            put("ssid", ssid)
            put("user_unique_id", userUniqueId)
            put("androidid", androidId)
            put("imei", imei)
            put("os_version", osVersion)
            put("device_model", deviceModel)
            put("google_aid", googleAid)
            put("click_time", clickTime)
            put("tr_shareuser", trShareUser)
            put("tr_admaster", trAdMaster)
            put("tr_param1", trParam1)
            put("tr_param2", trParam2)
            put("tr_param3", trParam3)
            put("tr_param4", trParam4)
            put("ab_version", abVersion)
            put("tr_web_ssid", webSsid)
        }
    }

    override fun initWithJson(json: JSONObject?) {
        json?.run {
            token = optString("tr_token", null)
            aid = optString("aid", null)
            os = optString("os", null)
            bdDid = optString("bd_did", null)
            ssid = optString("ssid", null)
            userUniqueId = optString("user_unique_id", null)
            androidId = optString("androidid", null)
            imei = optString("imei", null)
            osVersion = optString("os_version", null)
            deviceModel = optString("device_model", null)
            googleAid = optString("google_aid", null)
            clickTime = optInt("click_time")
            trShareUser = optString("tr_shareuser", null)
            trAdMaster = optString("tr_admaster", null)
            trParam1 = optString("tr_param1", null)
            trParam2 = optString("tr_param2", null)
            trParam3 = optString("tr_param3", null)
            trParam4 = optString("tr_param4", null)
            abVersion = optString("ab_version", null)
            webSsid = optString("tr_web_ssid", null)
        }
    }
}