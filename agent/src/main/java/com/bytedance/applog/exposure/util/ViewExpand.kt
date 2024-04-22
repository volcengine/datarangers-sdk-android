// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.exposure.util

import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.widget.ImageView
import com.bytedance.applog.exposure.DebugDrawable
import com.bytedance.applog.util.ViewHelper


/**
 * 检测 View 是否在屏幕的可视区域内
 */
fun View.isVisibleInViewport(areaRatio: Float?): Boolean {
    if (ViewHelper.isViewVisibleInParents(this)) {
        val rect = Rect()
        val localVisibleRect = this.getLocalVisibleRect(rect)
        return localVisibleRect &&
                rect.width() * rect.height() >=
                this.measuredHeight * this.measuredWidth * (areaRatio ?: 0F)
    }
    return false
}

/**
 * 开启调试模式，给 View 增加一个红色边框
 */
fun View.enableViewExposureDebugMode() {
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
fun View.disableViewExposureDebugMode() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        if (this is ImageView && this.drawable is DebugDrawable) {
            this.setImageDrawable((this.drawable as DebugDrawable).wrappedDrawable)
        }
        if (this.background is DebugDrawable) {
            this.background = (this.background as DebugDrawable).wrappedDrawable
        }
    }
}

fun View.setViewExposureVisible(visible: Boolean) {
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

