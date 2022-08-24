// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.monitor

import com.bytedance.applog.monitor.model.BaseTrace

/**
 * 监控接口
 *
 * @author luodong.seu
 */
interface IMonitor {
    /**
     * 采集数据
     */
    fun trace(data: BaseTrace)

    /**
     * 上报数据
     */
    fun report()

}

/**
 * Nothing to do
 */
class NoMonitor : IMonitor {
    override fun trace(data: BaseTrace) {
    }

    override fun report() {
    }
}

