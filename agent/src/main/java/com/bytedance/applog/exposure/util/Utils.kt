// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.exposure.util

import com.bytedance.applog.AppLogInstance
import com.bytedance.applog.log.LogInfo

inline fun runSafely(appLog: AppLogInstance, task: () -> Unit) {
    try {
        task()
    } catch (e: Throwable) {
        appLog.logger.error(
            LogInfo.Category.VIEW_EXPOSURE, "Run task failed", e
        )
    }
}