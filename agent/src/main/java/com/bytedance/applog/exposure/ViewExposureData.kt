// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.exposure

import org.json.JSONObject

/**
 *
 * @author: baoyongzhang@bytedance.com
 * @date: 2022/3/30
 */

/**
 * View 曝光数据
 * @param eventName 自定义曝光事件名称，默认为 "$bav2b_exposure"
 * @param properties 自定义事件的 params 参数
 * @param config 自定义曝光的配置，只有当前 View 有效，全局配置可在 [com.bytedance.applog.InitConfig] 中配置
 */
data class ViewExposureData<Config : IExposureConfig>(
    val eventName: String? = null,
    val properties: JSONObject? = null,
    var config: Config? = null
)

interface IExposureConfig

/**
 * 用于保存待检测 View 的相关信息
 * @param data 曝光的相关数据
 * @param viewExposureTriggerType 记录上次曝光状态
 */
internal class ViewExposureHolder(
    val data: ViewExposureData<ViewExposureConfig>,
    var lastVisible: Boolean = false,
    var viewExposureTriggerType: ViewExposureTriggerType = ViewExposureTriggerType.NOT_EXPOSURE,
    var lastVisibleTime: Long = 0
) {
    fun updateViewExposureTriggerTypeAndRestLastVisible(isFromBack: Boolean) {
        viewExposureTriggerType =
            if (viewExposureTriggerType != ViewExposureTriggerType.NOT_EXPOSURE) {
                if (isFromBack) {
                    ViewExposureTriggerType.RESUME_FORM_BACK
                } else {
                    ViewExposureTriggerType.RESUME_FORM_PAGE
                }
            } else {
                ViewExposureTriggerType.NOT_EXPOSURE
            }

        lastVisible = false
        lastVisibleTime = 0
    }

    fun updateLastVisibleTime(isVisibleInViewport: Boolean) {
        lastVisibleTime = if (isVisibleInViewport) {
            if (lastVisibleTime == 0L) {
                System.currentTimeMillis()
            } else {
                lastVisibleTime
            }
        } else {
            0
        }
    }

    fun checkVisibleTime(data: ViewExposureData<ViewExposureConfig>): Boolean {
        return System.currentTimeMillis() - lastVisibleTime >= (data.config?.stayTriggerTime ?: 0)
    }
}

enum class ViewExposureTriggerType(val value: Int) {

    // 首次
    EXPOSURE_ONCE(0),

    // 页面生命周期内多次曝光
    EXPOSURE_MORE_THAN_ONCE(3),

    // 从其他页面返回
    RESUME_FORM_PAGE(6),

    // 从后台返回
    RESUME_FORM_BACK(7),

    // 未触发
    NOT_EXPOSURE(-1);
}