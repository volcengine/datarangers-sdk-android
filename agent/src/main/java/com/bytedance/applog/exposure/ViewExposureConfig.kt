// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.exposure

import androidx.annotation.Keep
import org.json.JSONObject

/**
 * View 曝光配置
 * @param areaRatio 有效曝光的 View 显示面积的比例，取值 (0-1]
 * @param visualDiagnosis 是否开启曝光视觉辅助功能
 * @param stayTriggerTime 触发曝光的停留时长，单位 ms
 * @param exposureCallback 曝光回调，参数 ViewExposureParam，返回值是否保留
 */
data class ViewExposureConfig @JvmOverloads constructor(
    // 曝光面积比例
    val areaRatio: Float? = null,
    // 是否开启视觉调试
    val visualDiagnosis: Boolean? = false,
    // 曝光触发的停留时长
    val stayTriggerTime: Long = 0,
    // 曝光回调 true 为保留事件 false 为过滤事件 默认保留
    val exposureCallback: (ViewExposureParam) -> Boolean = { true }
) : IExposureConfig

@Keep
data class ViewExposureParam(
    // 原始曝光事件属性
    val exposureParam: JSONObject = JSONObject()
    // 后续可扩展其他内容
)

internal fun ViewExposureConfig.copyWith(from: ViewExposureConfig?) = ViewExposureConfig(
    from?.areaRatio ?: this.areaRatio,
    from?.visualDiagnosis ?: this.visualDiagnosis,
    from?.stayTriggerTime ?: this.stayTriggerTime,
    from?.exposureCallback ?: this.exposureCallback
)
