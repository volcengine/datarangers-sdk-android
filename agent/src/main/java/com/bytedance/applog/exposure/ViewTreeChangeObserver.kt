// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.exposure

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import com.bytedance.applog.R
import java.lang.ref.WeakReference

/**
 *
 * @author: baoyongzhang@bytedance.com
 * @date: 2022/3/30
 */
typealias ViewTreeChangeCallback = (Activity) -> Unit
typealias ActivityStoppedCallback = ((Activity?, Boolean) -> Unit)

/**
 * ViewTree 变化观察者，以 Activity 为粒度进行检测
 * 当当前 Activity 的 ViewTree 发生变动会触发回调
 */
class ViewTreeChangeObserver(val application: Application) :
    Application.ActivityLifecycleCallbacks, View.OnAttachStateChangeListener {

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
    private var onActivityStoppedCallback: ActivityStoppedCallback? = null

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

    fun registerActivityStoppedCallback(callback: ActivityStoppedCallback) {
        onActivityStoppedCallback = callback
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivityRef = WeakReference(activity)
        val rootView = activity.window.decorView
        observeViewTree(rootView)
    }

    /**
     * 这里去检查一下 View.rootView 是否已经注册了监听，因为如果是 Dialog 的话，和 Activity 不在一个 ViewTree 下，这里要单独去注册监听
     */
    fun checkObserveViewTree(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && view.isAttachedToWindow) {
            observeViewTree(view.rootView)
        } else {
            view.addOnAttachStateChangeListener(this)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        val rootView = activity.window.decorView
        disposeViewTree(rootView)
    }

    private fun observeViewTree(rootView: View) {
        if (rootView.getTag(R.id.applog_tag_view_exposure_observe_flag) == true) {
            return
        }
        rootView.setTag(R.id.applog_tag_view_exposure_observe_flag, true)

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

    private fun disposeViewTree(rootView: View) {
        if (rootView.getTag(R.id.applog_tag_view_exposure_observe_flag) != true) {
            return
        }
        rootView.setTag(R.id.applog_tag_view_exposure_observe_flag, false)
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
        currentActivityRef.get()?.let { resumeActivity ->
            onActivityStoppedCallback?.invoke(activity, resumeActivity == activity)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {

    }

    override fun onViewAttachedToWindow(view: View?) {
        view ?: return
        observeViewTree(view.rootView)
        view.removeOnAttachStateChangeListener(this)
    }

    override fun onViewDetachedFromWindow(v: View?) {
    }
}