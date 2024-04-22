// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.exposure.scroll

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.bytedance.applog.AppLogInstance
import com.bytedance.applog.exposure.ViewExposureData
import com.bytedance.applog.exposure.ViewExposureParam
import com.bytedance.applog.exposure.util.runSafely
import com.bytedance.applog.log.LogInfo
import com.bytedance.applog.util.JsonUtils
import com.bytedance.applog.util.ViewHelper
import org.json.JSONObject

class ScrollExposureHelper(private val appLog: AppLogInstance) {

    companion object {
        const val DEFAULT_MIN_SCROLL_DISTANCE = 30

        const val SCROLL_UP = 1     // 向上滚动
        const val SCROLL_DOWN = 2   // 向下滚动
        const val SCROLL_LEFT = 3   // 向左滚动
        const val SCROLL_RIGHT = 4  // 向右滚动
    }

    private val globalConfig = ScrollObserveConfig()
    val defaultData = ViewExposureData(config = globalConfig)

    fun observeViewScroll(
        view: RecyclerView,
        data: ViewExposureData<ScrollObserveConfig> = defaultData
    ) = runSafely(appLog) {
        if (appLog.initConfig?.isScrollObserveEnabled == true) {
            val minOffset = data.config?.minOffset ?: DEFAULT_MIN_SCROLL_DISTANCE
            view.addOnScrollListener(RecycleViewScrollListener(minOffset) { dx, dy, direction ->
                sendScrollExposure(view, data, dx, dy, direction)
            })
        } else {
            appLog.logger.warn(
                "[ScrollExposure] observeScrollExposure failed isScrollExposureEnabled false."
            )
        }
    }

    fun observeViewScroll(
        view: ViewPager,
        data: ViewExposureData<ScrollObserveConfig> = defaultData
    ) = runSafely(appLog) {
        if (appLog.initConfig?.isScrollObserveEnabled == true) {
            val minOffset = data.config?.minOffset ?: DEFAULT_MIN_SCROLL_DISTANCE
            view.addOnPageChangeListener(ViewPagerScrollListener(minOffset) { dx, dy, direction ->
                sendScrollExposure(view, data, dx, dy, direction)
            })
        } else {
            appLog.logger.warn(
                "[ScrollExposure] observeScrollExposure failed isScrollExposureEnabled false."
            )
        }
    }

    private fun sendScrollExposure(
        view: View,
        data: ViewExposureData<ScrollObserveConfig>,
        dx: Float,
        dy: Float,
        direction: Int
    ) {
        val eventName = data.eventName ?: "\$bav2b_slide"
        // 填充曝光事件属性，这里预置属性和 Click 保持一致，不包含 touchX 和 touchY
        val viewInfo = ViewHelper.getClickViewInfo(view, true)
        val params = JSONObject()
        try {
            params.put("page_key", viewInfo.page)
            params.put("page_title", viewInfo.pageTitle)
            params.put("element_path", viewInfo.path)
            params.put("element_width", viewInfo.width)
            params.put("element_height", viewInfo.height)
            params.put("element_id", viewInfo.elementId)
            params.put("element_type", viewInfo.elementType)
            params.put("\$offsetX", dx)
            params.put("\$offsetY", dy)
            params.put("\$direction", direction)
            // 合并用户自定义属性
            data.properties?.let {
                JsonUtils.mergeJsonObject(it, params)
            }
        } catch (e: Exception) {
            appLog.logger.error(
                LogInfo.Category.VIEW_EXPOSURE, "[ScrollExposure] JSON handle failed", e
            )
        }
        val callback = data.config?.scrollCallback ?: globalConfig.scrollCallback
        if (callback.invoke(ViewExposureParam(params))) {
            // 不过滤上报
            appLog.onEventV3(eventName, params)
        } else {
            appLog.logger.warn("[ScrollExposure] filter sendScrollExposure event $eventName, $params")
        }
    }
}