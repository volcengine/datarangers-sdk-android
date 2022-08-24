// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.exposure

import org.json.JSONObject

/**
 *
 * @author: baoyongzhang@bytedance.com
 * @date: 2022/3/30
 */

/**
 * View 曝光配置
 * @param areaRatio 有效曝光的 View 显示面积的比例，取值 0-1
 */
data class ViewExposureConfig(
    val areaRatio: Float? = null,
    val visualDiagnosis: Boolean? = false
)

internal fun ViewExposureConfig.copyWith(from: ViewExposureConfig?) = ViewExposureConfig(
    from?.areaRatio ?: this.areaRatio,
    from?.visualDiagnosis ?: this.visualDiagnosis
)


/**
 * View 曝光数据
 * @param eventName 自定义曝光事件名称，默认为 "$bav2b_exposure"
 * @param properties 自定义事件的 params 参数
 * @param config 自定义曝光的配置，只有当前 View 有效，全局配置可在 [com.bytedance.applog.InitConfig] 中配置
 */
data class ViewExposureData(
    val eventName: String? = null,
    val properties: JSONObject? = null,
    var config: ViewExposureConfig? = null
)

/**
 * 用于保存待检测 View 的相关信息
 * @param data 曝光的相关数据
 * @param lastVisible 记录上次显示状态，防止重复曝光
 */
internal class ViewExposureHolder(val data: ViewExposureData, var lastVisible: Boolean = false)