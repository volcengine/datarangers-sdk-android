// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.exposure.task

import com.bytedance.applog.exposure.ExposureCheckType
import com.bytedance.applog.exposure.ViewExposureManager

class ViewExposureTask(private val manager: ViewExposureManager) {

    private val checkTask = Runnable {
        val activity = manager.getCurrActivity() ?: return@Runnable
        manager.checkViewExposureFromActivity(activity)
    }

    var checkStrategy: BaseCheckExposureStrategy = DebounceCheckExposureStrategy(checkTask)

    fun updateExposureCheckStrategy(exposureCheckType: ExposureCheckType?) {
        checkStrategy = when (exposureCheckType) {
            ExposureCheckType.THROTTLE -> ThrottleCheckExposureStrategy(checkTask)
            else -> DebounceCheckExposureStrategy(checkTask)
        }
    }

    fun check() {
        checkStrategy.check()
    }
}


