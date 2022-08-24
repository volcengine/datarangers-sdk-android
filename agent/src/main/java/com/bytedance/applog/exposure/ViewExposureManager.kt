// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.exposure

import android.app.Activity
import android.app.Application
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import com.bytedance.applog.AppLogInstance
import com.bytedance.applog.util.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

/**
 * View 曝光事件管理类，负责注册和取消对 View 的曝光监听
 * @author: baoyongzhang@bytedance.com
 * @date: 2022/3/30
 */
class ViewExposureManager(val appLog: AppLogInstance) {

    companion object {
        private val DEFAULT_CONFIG = ViewExposureConfig(1F)
        private const val DEFAULT_CHECK_DELAY = 100L
    }

    private val activitiesMap = WeakHashMap<Activity, WeakHashMap<View, ViewExposureHolder>>()
    private var started = false
    private var viewTreeChangeObserver = ViewTreeChangeObserver(appLog.context as Application)
    private val globalConfig = appLog.initConfig?.exposureConfig ?: DEFAULT_CONFIG
    private val mainHandler = Handler(Looper.getMainLooper())
    private val checkTask = Runnable {
        val activity = viewTreeChangeObserver.getCurrentActivity() ?: return@Runnable
        checkViewExposureFromActivity(activity)
    }

    init {
        if (appLog.initConfig?.isExposureEnabled == true) {
            start()
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
    fun observeViewExposure(view: View, data: ViewExposureData?) = runSafely {
        if (appLog.initConfig?.isExposureEnabled != true) {
            TLog.w("[ViewExposure] observe failed: InitConfig.exposureEnabled is not true.")
            return@runSafely
        }
        val activity = ActivityUtil.findActivity(view)
        if (activity == null) {
            TLog.w("[ViewExposure] observe failed: The view context is not Activity.")
            return@runSafely
        }
        if (ViewUtils.isIgnoredView(view)) {
            TLog.w("[ViewExposure] observe failed: The view is ignored.")
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

        TLog.d("[ViewExposure] observe successful, data=${data}, view=${view}")
    }

    /**
     * 取消监听 View 的曝光事件
     */
    fun disposeViewExposure(view: View) = runSafely {
        val activity = ActivityUtil.findActivity(view) ?: return@runSafely
        val holder = activitiesMap[activity]?.remove(view) ?: return@runSafely
        if (holder.data.config?.visualDiagnosis == true) {
            view.disableViewExposureDebugMode()
        }
    }


    private fun checkViewExposureFromActivity(activity: Activity) = runSafely {
//        TLog.d("[ViewExposure] checkViewExposureFromActivity")
        val weakHashMap = activitiesMap[activity] ?: return@runSafely
        weakHashMap.forEach {
            val view = it.key
            val holder = it.value
            val data = holder.data
            // 判断下显示状态是否发生变动，防止重复发送曝光事件
            if (holder.lastVisible != view.isVisibleInViewport(data.config?.areaRatio)) {
                if (!holder.lastVisible) {
                    // 从未显示到显示，触发曝光
                    sendViewExposureEvent(view, data)
                    holder.lastVisible = true
                } else {
                    // 从显示到未显示
                    holder.lastVisible = false
                }
                if (data.config?.visualDiagnosis == true) {
                    view.setViewExposureVisible(holder.lastVisible)
                }
                TLog.d("[ViewExposure] visible change to ${holder.lastVisible}, config=${data.config} view=${view}")
            }
        }
    }

    private fun sendViewExposureEvent(view: View, data: ViewExposureData?) = runSafely {
        val eventName = data?.eventName ?: "\$bav2b_exposure"
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
            // 合并用户自定义属性
            data?.properties?.let {
                JsonUtils.mergeJsonObject(it, params)
            }
        } catch (e: Exception) {
            TLog.e(e)
        }
        appLog.onEventV3(eventName, params)
    }

    private fun start() {
        if (started) {
            return
        }
        viewTreeChangeObserver.subscribe {
            mainHandler.removeCallbacks(checkTask)
            mainHandler.postDelayed(checkTask, DEFAULT_CHECK_DELAY)
        }
        started = true
    }
}

/**
 * 检测 View 是否在屏幕的可视区域内
 */
private fun View.isVisibleInViewport(areaRatio: Float?): Boolean {
    if (ViewHelper.isViewVisibleInParents(this)) {
        val rect = Rect()
        val localVisibleRect = this.getLocalVisibleRect(rect)
//        TLog.d("[ViewExposure] getLocalVisibleRect $rect, ${rect.width()}, ${rect.height()}")
        return localVisibleRect &&
                rect.width() * rect.height() >=
                this.measuredHeight * this.measuredWidth * (areaRatio ?: 0F)
    }
    return false
}

/**
 * 开启调试模式，给 View 增加一个红色边框
 */
private fun View.enableViewExposureDebugMode() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        if (this is ImageView) {
            this.setImageDrawable(DebugDrawable(this.drawable))
        }
        this.background = DebugDrawable(this.background)
    }
}

/**
 * 关闭调试模式
 */
private fun View.disableViewExposureDebugMode() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        if (this is ImageView && this.drawable is DebugDrawable) {
            this.setImageDrawable((this.drawable as DebugDrawable).wrappedDrawable)
        }
        if (this.background is DebugDrawable) {
            this.background = (this.background as DebugDrawable).wrappedDrawable
        }
    }
}

private fun View.setViewExposureVisible(visible: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        val color = if (visible) Color.RED else Color.YELLOW
        if (this is ImageView && this.drawable is DebugDrawable) {
            (this.drawable as DebugDrawable).setBorderColor(color)

        }
        if (this.background is DebugDrawable) {
            (this.background as DebugDrawable).setBorderColor(color)
        }
        this.invalidate()
    }
}

private fun runSafely(task: () -> Unit) {
    try {
        task()
    } catch (e: Throwable) {
        TLog.e(e)
    }
}

/**
 * 用于调试开启曝光检测的 View，会给 View 增加一个红色边框的背景
 */
private class DebugDrawable(drawable: Drawable?) : DrawableWrapper(drawable) {

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