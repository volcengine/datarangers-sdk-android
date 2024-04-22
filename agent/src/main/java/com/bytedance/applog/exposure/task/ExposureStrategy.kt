// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.exposure.task

import android.os.Handler
import android.os.Looper

private const val DEBOUNCE_CHECK_DELAY = 100L
private const val THROTTLE_CHECK_DELAY = 500L

interface ICheckExposureStrategy {
    fun check()
}

abstract class BaseCheckExposureStrategy : ICheckExposureStrategy {
    val mainHandler = Handler(Looper.getMainLooper())
}

class DebounceCheckExposureStrategy(private val checkTask: Runnable) : BaseCheckExposureStrategy() {

    override fun check() {
        mainHandler.removeCallbacks(checkTask)
        mainHandler.postDelayed(checkTask, DEBOUNCE_CHECK_DELAY)
    }
}

class ThrottleCheckExposureStrategy(private val checkTask: Runnable) : BaseCheckExposureStrategy() {
    @Volatile
    private var isCheckFinish = true

    override fun check() {
        if (isCheckFinish) {
            isCheckFinish = false
            mainHandler.removeCallbacks(checkTask)
            mainHandler.postDelayed({
                checkTask.run()
                isCheckFinish = true
            }, THROTTLE_CHECK_DELAY)
        }
    }
}

