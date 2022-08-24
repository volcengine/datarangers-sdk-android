// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.monitor

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import com.bytedance.applog.engine.Engine
import com.bytedance.applog.monitor.model.BaseTrace
import com.bytedance.applog.store.Trace
import org.json.JSONObject

private const val MSG_TRACE = 1
private const val MSG_REPORT = 2

/**
 * 监控实现类
 *
 * @author luodong.seu
 */
class MonitorImpl(private val mEngine: Engine) : IMonitor, Handler.Callback, TraceAggregation {

    private var mHandler: Handler

    init {
        val handlerThread = HandlerThread("bd_tracker_monitor@" + mEngine.appLog.appId)
        handlerThread.start()
        mHandler = Handler(handlerThread.looper, this)
    }

    override fun trace(data: BaseTrace) {
        if (!mEngine.config.isMonitorEnabled) {
            return
        }
        val trace = Trace()
        mEngine.session.fillSessionParams(mEngine.appLog, trace)
        trace.setProperties(data.getTraceParams())
        mHandler.sendMessage(mHandler.obtainMessage(MSG_TRACE, trace))

    }

    override fun report() {
        if (!mEngine.config.isMonitorEnabled) {
            return
        }
        mHandler.sendEmptyMessage(MSG_REPORT)
    }

    override fun aggregate(dataList: List<Trace>): List<Trace> {
        val result = mutableListOf<Trace>()

        // 聚合api calls
        val apiCallTraceMaps = mutableMapOf<String, Trace>()

        // 聚合数据
        for (trace in dataList) {
            val properties = trace.getProperties()
            val category = properties.optString(BaseTrace.CATEGORY_KEY)
            val name = properties.optString(BaseTrace.NAME_KEY)
            if (category == "data_statistics") {
                when (name) {
                    "api_calls" -> {
                        val funName = properties.optString("api_name")
                        val savedTrace = apiCallTraceMaps[funName]
                        if (null == savedTrace) {
                            apiCallTraceMaps[funName] = trace
                            result.add(trace)
                        } else {
                            val savedProps = savedTrace.getProperties() ?: JSONObject()
                            val count = savedProps.optInt(BaseTrace.VALUE_KEY, 0) +
                                    properties.optInt(BaseTrace.VALUE_KEY, 1)
                            val time =
                                savedProps.optLong("api_time", 0) +
                                        properties.optLong("api_time", 0)
                            savedProps.put(
                                BaseTrace.VALUE_KEY, count
                            )
                            savedProps.put(
                                "api_time", time
                            )
                        }
                    }
                }
            } else {
                result.add(trace)
            }
        }
        return result
    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            MSG_TRACE -> {
                mEngine.dbStoreV2.saveAll(listOf(msg.obj as Trace))
            }
            MSG_REPORT -> {
                // 上报数据
                mEngine.dbStoreV2.packTrace(mEngine.appLog.appId, mEngine.dm.header, this)
                mEngine.sendLogImmediately()
            }
        }
        return true
    }
}