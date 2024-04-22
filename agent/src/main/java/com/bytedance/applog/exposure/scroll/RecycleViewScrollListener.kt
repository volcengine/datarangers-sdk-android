// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.exposure.scroll

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bytedance.applog.exposure.scroll.ScrollExposureHelper.Companion.SCROLL_DOWN
import com.bytedance.applog.exposure.scroll.ScrollExposureHelper.Companion.SCROLL_LEFT
import com.bytedance.applog.exposure.scroll.ScrollExposureHelper.Companion.SCROLL_RIGHT
import com.bytedance.applog.exposure.scroll.ScrollExposureHelper.Companion.SCROLL_UP
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class RecycleViewScrollListener(
    private val minOffset: Int,
    private val sendScrollObserveCallback: SendScrollObserveCallback
) : RecyclerView.OnScrollListener() {

    private var lastMaxScrollX = 0
    private var lastMaxScrollY = 0

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        // 滑动结束记录滑动事件
        if (newState == RecyclerView.SCROLL_STATE_IDLE
            && (abs(lastMaxScrollX) >= minOffset || abs(lastMaxScrollY) >= minOffset)
        ) {
            // 超过最小滑动距离记录
            sendScrollObserveCallback.invoke(
                lastMaxScrollX.toFloat(),
                lastMaxScrollY.toFloat(),
                calDirection(recyclerView)
            )
            lastMaxScrollX = 0
            lastMaxScrollY = 0
        }
    }

    private fun calDirection(view: RecyclerView) = when (view.layoutManager) {
        is LinearLayoutManager -> {
            when ((view.layoutManager as LinearLayoutManager).orientation) {
                RecyclerView.HORIZONTAL -> if (lastMaxScrollX > 0) {
                    SCROLL_RIGHT
                } else {
                    SCROLL_LEFT
                }

                RecyclerView.VERTICAL -> {
                    if (lastMaxScrollY > 0) {
                        SCROLL_DOWN
                    } else {
                        SCROLL_UP
                    }
                }

                else -> SCROLL_DOWN
            }

        }

        is StaggeredGridLayoutManager -> {
            if (lastMaxScrollY > 0) {
                SCROLL_DOWN
            } else {
                SCROLL_UP
            }
        }

        else -> SCROLL_DOWN
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        // 滑动过程中记录
        if (abs(dx) > minOffset || abs(dy) > minOffset) {
            lastMaxScrollX = if (dx > 0) {
                max(lastMaxScrollX, dx)
            } else {
                min(lastMaxScrollX, dx)
            }
            lastMaxScrollY = if (dy > 0) {
                max(lastMaxScrollY, dy)
            } else {
                min(lastMaxScrollY, dy)
            }
        }
    }
}