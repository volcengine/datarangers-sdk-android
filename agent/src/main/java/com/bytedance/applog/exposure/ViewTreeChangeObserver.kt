// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.exposure

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import java.lang.ref.WeakReference

/**
 *
 * @author: baoyongzhang@bytedance.com
 * @date: 2022/3/30
 */
typealias ViewTreeChangeCallback = (Activity) -> Unit

/**
 * ViewTree 变化观察者，以 Activity 为粒度进行检测
 * 当当前 Activity 的 ViewTree 发生变动会触发回调
 */
class ViewTreeChangeObserver(val application: Application) :
    Application.ActivityLifecycleCallbacks {

    private var currentActivityRef = WeakReference<Activity>(null)

    private var onDrawListener = ViewTreeObserver.OnDrawListener { invokeCallback() }

    private var onGlobalLayoutListener =
        ViewTreeObserver.OnGlobalLayoutListener { invokeCallback() }

    private var onGlobalFocusChangeListener =
        ViewTreeObserver.OnGlobalFocusChangeListener { _: View?, _: View? -> invokeCallback() }

    private var onScrollChangedListener =
        ViewTreeObserver.OnScrollChangedListener { invokeCallback() }

    private var onWindowFocusChangeListener =
        ViewTreeObserver.OnWindowFocusChangeListener { invokeCallback() }

    private var viewTreeChangeCallback: ViewTreeChangeCallback? = null

    private fun invokeCallback() {
        val activity = currentActivityRef.get() ?: return
        viewTreeChangeCallback?.invoke(activity)
    }

    /**
     * 获取当前 Activity 对象
     */
    fun getCurrentActivity() = currentActivityRef.get()

    /**
     * 订阅全局 ViewTree 的变化
     * 通过 [Application.ActivityLifecycleCallbacks] 会始终跟踪当前 resumed 的 Activity 的 ViewTree 变化
     * 当 onDraw、onGlobalLayout、onGlobalFocusChange、onScrollChanged、OnWindowFocusChange 的时候会触发回调
     */
    fun subscribe(callback: (Activity) -> Unit) {
        if (viewTreeChangeCallback == null) {
            viewTreeChangeCallback = callback
            application.registerActivityLifecycleCallbacks(this)
        }
    }

    /**
     * 取消订阅全局 ViewTree 的变化
     */
    fun unsubscribe() {
        if (viewTreeChangeCallback != null) {
            viewTreeChangeCallback = null
            application.unregisterActivityLifecycleCallbacks(this)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivityRef = WeakReference(activity)
        val rootView = activity.window.decorView
        val viewTreeObserver = rootView.viewTreeObserver
        // 注册各种 ViewTree 变化的监听
        viewTreeObserver.addOnGlobalFocusChangeListener(onGlobalFocusChangeListener)
        viewTreeObserver.addOnScrollChangedListener(onScrollChangedListener)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            viewTreeObserver.addOnDrawListener(onDrawListener)
            viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            viewTreeObserver.addOnWindowFocusChangeListener(onWindowFocusChangeListener)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        val rootView = activity.window.decorView
        val viewTreeObserver = rootView.viewTreeObserver
        // 取消注册各种 ViewTree 变化的监听
        viewTreeObserver.removeOnGlobalFocusChangeListener(onGlobalFocusChangeListener)
        viewTreeObserver.removeOnScrollChangedListener(onScrollChangedListener)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            viewTreeObserver.removeOnDrawListener(onDrawListener)
            viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            viewTreeObserver.removeOnWindowFocusChangeListener(onWindowFocusChangeListener)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {

    }

    override fun onActivityStopped(activity: Activity) {

    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {

    }
}