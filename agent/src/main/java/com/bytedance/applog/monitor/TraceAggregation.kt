// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.monitor

import com.bytedance.applog.store.Trace

interface TraceAggregation {
    /**
     * 聚合数据
     */
    fun aggregate(dataList: List<Trace>): List<Trace>
}