// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.exposure.scroll

import androidx.viewpager.widget.ViewPager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ViewPagerScrollListener(
    private val minOffset: Int,
    private val sendScrollObserveCallback: SendScrollObserveCallback
) : ViewPager.OnPageChangeListener {
    private var lastMaxScrollX = 0
    private var initScrollX = -1

    override fun onPageScrolled(
        position: Int,
        positionOffset: Float,
        positionOffsetPixels: Int
    ) {
        // 滑动过程中记录
        if (abs(positionOffsetPixels) > minOffset || abs(0) > minOffset) {
            lastMaxScrollX = if (positionOffsetPixels > 0) {
                max(lastMaxScrollX, positionOffsetPixels)
            } else {
                min(lastMaxScrollX, positionOffsetPixels)
            }
        }
        if (initScrollX == -1) {
            initScrollX = lastMaxScrollX
        }
    }

    override fun onPageSelected(position: Int) {}

    override fun onPageScrollStateChanged(state: Int) {
        when (state) {
            // 滑动结束记录滑动事件
            ViewPager.SCROLL_STATE_IDLE -> {
                if (abs(lastMaxScrollX) >= minOffset) {
                    // 超过最小滑动距离记录
                    sendScrollObserveCallback.invoke(
                        lastMaxScrollX.toFloat(),
                        0f,
                        calDirection()
                    )
                    lastMaxScrollX = 0
                    initScrollX = -1
                }
            }
        }
    }

    private fun calDirection() = if (lastMaxScrollX > initScrollX) {
        ScrollExposureHelper.SCROLL_RIGHT
    } else {
        ScrollExposureHelper.SCROLL_LEFT
    }
}