// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.alink.model

import org.json.JSONObject
import java.util.*


/**
 * 归因数据
 * @author chenguanzhong
 * @date 2021/1/27
 **/
class AttributionData : BaseData() {
    enum class ActivationType { PROMOTION, ORGANIC }

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
    var trDp: String? = null
    var deeplinkValue: String? = null

    var trSiteId: String? = null
    var trSiteName: String? = null
    var accountId: String? = null
    var accountName: String? = null
    var campaignId: String? = null
    var campaignName: String? = null
    var adId: String? = null
    var adName: String? = null
    var creativeId: String? = null
    var creativeName: String? = null

    var trInstallType: String? = null
    var touchType: String? = null
    var touchTimestamp: String? = null
    var activationType: ActivationType = ActivationType.PROMOTION
    var activationTimestamp: String? = null
    var isFirstLaunch: Boolean = false


    override fun toJson(): JSONObject {
        return JSONObject().apply {
            put("name", name)
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
            put("is_retargeting", isRetargeting)
            put("reengagement_window", reengagementWindow)
            put("tr_dp", trDp)
            put("deeplink_value", deeplinkValue)
            put("tr_site_id", trSiteId)
            put("tr_site_name", trSiteName)
            put("account_id", accountId)
            put("account_name", accountName)
            put("campaign_id", campaignId)
            put("campaign_name", campaignName)
            put("ad_id", adId)
            put("ad_name", adName)
            put("creative_id", creativeId)
            put("creative_name", creativeName)
            put("tr_install_type", trInstallType)
            put("touch_type", touchType)
            put("touch_timestamp", touchTimestamp)
            put("activation_type", activationType.name.toLowerCase(Locale.ROOT))
            put("activation_timestamp", activationTimestamp)
            put("is_first_launch", isFirstLaunch)
        }
    }

    override fun initWithJson(json: JSONObject?) {
        json?.run {
            name = optString("name", null)
            utmCampaign = optString("utm_campaign", null)
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
            trDp = optString("tr_dp", null)
            deeplinkValue = optString("deeplink_value", null)
            trSiteId = optString("tr_site_id", null)
            trSiteName = optString("tr_site_name", null)
            accountId = optString("account_id", null)
            accountName = optString("account_name", null)
            campaignId = optString("campaign_id", null)
            campaignName = optString("campaign_name", null)
            adId = optString("ad_id", null)
            adName = optString("ad_name", null)
            creativeId = optString("creative_id", null)
            creativeName = optString("creative_name", null)
            trInstallType = optString("tr_install_type", null)
            touchType = optString("touch_type", null)
            touchTimestamp = optString("touch_timestamp", null)
            activationType =
                if (optString("activation_type") == "promotion") ActivationType.PROMOTION else ActivationType.ORGANIC
            activationTimestamp = optString("activation_timestamp", null)
            isFirstLaunch = optBoolean("is_first_launch")
        }
    }
}