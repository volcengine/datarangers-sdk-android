// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.alink

import androidx.annotation.WorkerThread

interface IALinkListener {

    @WorkerThread
    fun onALinkData(routingInfo: Map<String, String?>?, exception: Exception?)

    @WorkerThread
    fun onAttributionData(routingInfo: Map<String, String?>?, exception: Exception?)
}