// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.alink.model

import org.json.JSONObject


/**
 * 链接参数
 * @author chenguanzhong
 * @date 2021/1/27
 **/
class ALinkData : BaseData() {
    var name: String? = null // 链接名称，
    var utmCampaign: String? = null //即推广活动名称
    var utmSource: String? = null
    var utmMedium: String? = null
    var utmContent: String? = null
    var utmTerm: String? = null
    var trShareuser: String? = null
    var trAdmaster: String? = null
    var trParam1: String? = null
    var trParam2: String? = null
    var trParam3: String? = null
    var trParam4: String? = null

    var isRetargeting: Boolean = false
    var reengagementWindow: Int = 0
    var reengagementTime: Long = 0
    var trDp: String? = null
    var deeplinkValue: String? = null

    //记录缓存token
    var token: String? = null

    override fun toJson(): JSONObject {
        return JSONObject().apply {
            put("name", name)
            //merge时加上前缀re
            put("utm_campaign", utmCampaign)
            put("utm_source", utmSource)
            put("utm_medium", utmMedium)
            put("utm_content", utmContent)
            put("utm_term", utmTerm)
            put("tr_shareuser", trShareuser)
            put("tr_admaster", trAdmaster)
            put("tr_param1", trParam1)
            put("tr_param2", trParam2)
            put("tr_param3", trParam3)
            put("tr_param4", trParam4)
            //merge时加上前缀re
            put("tr_dp", trDp)
            put("is_retargeting", isRetargeting)
            put("reengagement_window", reengagementWindow)
            put("reengagement_time", reengagementTime)
            put("deeplink_value", deeplinkValue)
            put("token", token)
        }
    }

    override fun initWithJson(json: JSONObject?) {
        json?.run {
            name = optString("name", null)
            utmCampaign = optString("utm_campaign",null)
            utmSource = optString("utm_source", null)
            utmMedium = optString("utm_medium", null)
            utmContent = optString("utm_content", null)
            utmTerm = optString("utm_term", null)
            trShareuser = optString("tr_shareuser", null)
            trAdmaster = optString("tr_admaster", null)
            trParam1 = optString("tr_param1", null)
            trParam2 = optString("tr_param2", null)
            trParam3 = optString("tr_param3", null)
            trParam4 = optString("tr_param4", null)
            isRetargeting = optBoolean("is_retargeting")
            reengagementWindow = optInt("reengagement_window")
            reengagementTime = optLong("reengagement_time")
            trDp = optString("tr_dp", null)
            deeplinkValue = optString("deeplink_value", null)
            token = optString("token", null)
        }
    }
}