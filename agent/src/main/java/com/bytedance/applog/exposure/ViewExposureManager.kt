// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.exposure

import android.app.Activity
import android.app.Application
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.bytedance.applog.AppLogInstance
import com.bytedance.applog.exposure.scroll.ScrollObserveConfig
import com.bytedance.applog.exposure.scroll.ScrollExposureHelper
import com.bytedance.applog.exposure.task.ViewExposureTask
import com.bytedance.applog.exposure.util.disableViewExposureDebugMode
import com.bytedance.applog.exposure.util.enableViewExposureDebugMode
import com.bytedance.applog.exposure.util.isVisibleInViewport
import com.bytedance.applog.exposure.util.runSafely
import com.bytedance.applog.exposure.util.setViewExposureVisible
import com.bytedance.applog.log.LogInfo
import com.bytedance.applog.util.ActivityUtil
import com.bytedance.applog.util.JsonUtils
import com.bytedance.applog.util.ViewHelper
import com.bytedance.applog.util.ViewUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.WeakHashMap

/**
 * View 曝光事件管理类，负责注册和取消对 View 的曝光监听
 * @author: baoyongzhang@bytedance.com
 * @date: 2022/3/30
 */
class ViewExposureManager(private val appLog: AppLogInstance) {

    companion object {
        private val DEFAULT_CONFIG = ViewExposureConfig(1F)
    }

    private val activitiesMap = WeakHashMap<Activity, WeakHashMap<View, ViewExposureHolder>>()
    private var started = false
    private var viewTreeChangeObserver = ViewTreeChangeObserver(appLog.context as Application)
    private var globalConfig = DEFAULT_CONFIG
    private val task by lazy {
        ViewExposureTask(this)
    }
    private val scrollExposureHelper by lazy {
        ScrollExposureHelper(appLog)
    }

    init {
        if (appLog.initConfig?.isExposureEnabled == true) {
            start()
        } else {
            appLog.logger.warn(
                "[ViewExposure] init failed isExposureEnabled false."
            )
        }
    }

    /**
     * 监听 View 的曝光事件
     * @param view 需要监听的 View 对象
     */
    fun observeViewExposure(view: View) {
        observeViewExposure(view, null)
    }

    /**
     * 监听 View 的曝光事件
     * @param view 需要监听的 View 对象
     * @param data 曝光事件的自定义信息
     */
    fun observeViewExposure(view: View, data: ViewExposureData<ViewExposureConfig>?) = runSafely(appLog) {
        if (appLog.initConfig?.isExposureEnabled != true) {
            appLog.logger.warn(
                LogInfo.Category.VIEW_EXPOSURE,
                "[ViewExposure] observe failed: InitConfig.exposureEnabled is not true."
            )
            return@runSafely
        }
        val activity = ActivityUtil.findActivity(view)
        if (activity == null) {
            appLog.logger.warn(
                LogInfo.Category.VIEW_EXPOSURE,
                "[ViewExposure] observe failed: The view context is not Activity."
            )
            return@runSafely
        }
        if (ViewUtils.isIgnoredView(view)) {
            appLog.logger.warn(
                LogInfo.Category.VIEW_EXPOSURE,
                "[ViewExposure] observe failed: The view is ignored."
            )
            return@runSafely
        }
        var weakHashMap = activitiesMap[activity]
        if (weakHashMap == null) {
            weakHashMap = WeakHashMap()
            activitiesMap[activity] = weakHashMap
        }
        val config = globalConfig.copyWith(data?.config)
        val exposureData =
            ViewExposureData(data?.eventName, data?.properties, config)
        weakHashMap[view] = ViewExposureHolder(exposureData)

        if (config.visualDiagnosis == true) {
            view.enableViewExposureDebugMode()
        }

        // 立即触发一次检测
        checkViewExposureFromActivity(activity)

        // 这里需要检查一下 View.rootView 是否已经注册了监听，因为如果是 Dialog 的话，和 Activity 不在一个 ViewTree 下，这里要单独去注册监听
        viewTreeChangeObserver.checkObserveViewTree(view)

        appLog.logger.debug(
            LogInfo.Category.VIEW_EXPOSURE,
            "[ViewExposure] observe successful, data=${data}, view=${view}"
        )
    }

    /**
     * 取消监听 View 的曝光事件
     */
    fun disposeViewExposure(view: View) = runSafely(appLog) {
        val activity = ActivityUtil.findActivity(view) ?: return@runSafely
        val holder = activitiesMap[activity]?.remove(view) ?: return@runSafely
        if (holder.data.config?.visualDiagnosis == true) {
            view.disableViewExposureDebugMode()
        }
    }

    /**
     * 检查 Activity 下是否有需要检测曝光的信息
     */
    internal fun checkViewExposureFromActivity(activity: Activity) = runSafely(appLog) {
        val weakHashMap = activitiesMap[activity] ?: return@runSafely
        weakHashMap.forEach {
            val view = it.key
            val holder = it.value
            val data = holder.data
            val isVisibleInViewport = view.isVisibleInViewport(data.config?.areaRatio)
            holder.updateLastVisibleTime(isVisibleInViewport)
            // 判断下显示状态是否发生变动，防止重复发送曝光事件
            if (holder.lastVisible != isVisibleInViewport) {
                if (!holder.lastVisible) {
                    if (holder.checkVisibleTime(data)) {
                        // 从未显示到显示，触发曝光
                        triggeredExposure(view, holder)
                    }
                } else {
                    // 从显示到未显示
                    holder.lastVisible = false
                }
                if (data.config?.visualDiagnosis == true) {
                    view.setViewExposureVisible(holder.lastVisible)
                }
                appLog.logger.debug(
                    LogInfo.Category.VIEW_EXPOSURE,
                    "[ViewExposure] visible change to ${holder.lastVisible}, exposureTriggerType=${holder.viewExposureTriggerType}, config=${data.config} view=${view}"
                )
            }
        }
    }

    /**
     * 触发曝光
     */
    private fun triggeredExposure(view: View, holder: ViewExposureHolder) {
        when (holder.viewExposureTriggerType) {
            // 未曝光 -> 首次曝光
            ViewExposureTriggerType.NOT_EXPOSURE -> {
                holder.viewExposureTriggerType =
                    ViewExposureTriggerType.EXPOSURE_ONCE
                sendViewExposureEvent(view, holder)
            }

            // 首次曝光 -> 多次曝光
            ViewExposureTriggerType.EXPOSURE_ONCE -> {
                holder.viewExposureTriggerType =
                    ViewExposureTriggerType.EXPOSURE_MORE_THAN_ONCE
                sendViewExposureEvent(view, holder)
            }

            // 从其他地方返回 先上报返回曝光 再切换为多次曝光
            ViewExposureTriggerType.RESUME_FORM_PAGE,
            ViewExposureTriggerType.RESUME_FORM_BACK -> {
                sendViewExposureEvent(view, holder)
                holder.viewExposureTriggerType =
                    ViewExposureTriggerType.EXPOSURE_MORE_THAN_ONCE
            }

            else -> {
                // 多次曝光直接上报
                sendViewExposureEvent(view, holder)
            }
        }
        holder.lastVisible = true
        holder.lastVisibleTime = 0
    }

    /**
     * 拼接曝光信息发送曝光事件
     */
    private fun sendViewExposureEvent(view: View, holder: ViewExposureHolder) = runSafely(appLog) {
        val data = holder.data
        val eventName = data.eventName ?: "\$bav2b_exposure"
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
            if (!viewInfo.positions.isNullOrEmpty()) {
                params.put("positions", JSONArray(viewInfo.positions))
            }
            if (!viewInfo.contents.isNullOrEmpty()) {
                params.put("texts", JSONArray(viewInfo.contents))
            }
            params.put("\$exposure_type", holder.viewExposureTriggerType.value)
            // 合并用户自定义属性
            data.properties?.let {
                JsonUtils.mergeJsonObject(it, params)
            }
        } catch (e: Exception) {
            appLog.logger.error(
                LogInfo.Category.VIEW_EXPOSURE, "[ViewExposure] JSON handle failed", e
            )
        }
        val callback = data.config?.exposureCallback ?: globalConfig.exposureCallback
        if (callback.invoke(ViewExposureParam(params))) {
            // 不过滤上报
            appLog.onEventV3(eventName, params)
        } else {
            appLog.logger.warn("[ViewExposure] filter sendViewExposureEvent event $eventName, $params")
        }
    }

    /**
     * 曝光检测开始
     */
    private fun start() {
        if (started) {
            return
        }
        viewTreeChangeObserver.subscribe {
            task.check()
        }
        viewTreeChangeObserver.registerActivityStoppedCallback { activity, isFromBack ->
            activity ?: return@registerActivityStoppedCallback
            //  针对当前 stop 周期的 activity 里面的 view，清空标志位，重新处于可曝光的状态
            val weakHashMap = activitiesMap[activity] ?: return@registerActivityStoppedCallback
            weakHashMap.forEach {
                it.value.updateViewExposureTriggerTypeAndRestLastVisible(isFromBack)
            }
        }
        started = true
    }

    /**
     * 更新曝光检测策略
     */
    fun updateExposureCheckStrategy(exposureCheckType: ExposureCheckType?) {
        task.updateExposureCheckStrategy(exposureCheckType)
    }

    /**
     * 更新全局曝光配置
     */
    fun updateViewExposureConfig(viewExposureConfig: ViewExposureConfig) {
        globalConfig = viewExposureConfig
    }

    /**
     * 获取当前 Activity
     */
    fun getCurrActivity() = viewTreeChangeObserver.getCurrentActivity()

    fun observeViewScroll(
        view: RecyclerView,
        data: ViewExposureData<ScrollObserveConfig> = scrollExposureHelper.defaultData
    ) {
        scrollExposureHelper.observeViewScroll(view, data)
    }

    fun observeViewScroll(
        view: ViewPager,
        data: ViewExposureData<ScrollObserveConfig> = scrollExposureHelper.defaultData
    ) {
        scrollExposureHelper.observeViewScroll(view, data)
    }
}

/**
 * 用于调试开启曝光检测的 View，会给 View 增加一个红色边框的背景
 */
internal class DebugDrawable(drawable: Drawable?) : DrawableWrapper(drawable) {

    private val paint = Paint()

    init {
        paint.color = Color.YELLOW
        paint.strokeWidth = 8f
        paint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.drawRect(Rect(0, 0, canvas.width, canvas.height), paint)
    }

    fun setBorderColor(color: Int) {
        paint.color = color
    }
}