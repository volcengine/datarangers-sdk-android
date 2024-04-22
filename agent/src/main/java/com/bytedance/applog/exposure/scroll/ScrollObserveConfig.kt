// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.exposure.scroll

import com.bytedance.applog.exposure.IExposureConfig
import com.bytedance.applog.exposure.scroll.ScrollExposureHelper.Companion.DEFAULT_MIN_SCROLL_DISTANCE
import com.bytedance.applog.exposure.ViewExposureParam

/**
 * View 曝光配置
 */
data class ScrollObserveConfig @JvmOverloads constructor(
    // 滑动事件最小偏移量
    val minOffset: Int = DEFAULT_MIN_SCROLL_DISTANCE,
    // 事件回调，可用于添加属性以及判断是否保留
    val scrollCallback: (ViewExposureParam) -> Boolean = { true }
) : IExposureConfig