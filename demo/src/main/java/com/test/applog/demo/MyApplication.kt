// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.test.applog.demo

import android.app.Application
import android.util.Log
import com.bytedance.applog.AppLog
import com.bytedance.applog.ILogger
import com.bytedance.applog.InitConfig
import com.bytedance.applog.UriConfig

/**
 *
 * @author: baoyongzhang
 * @date: 2022/6/27
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        /* 初始化开始 */
        val config = InitConfig("10000000", "test")
        config.uriConfig =
            UriConfig.createByDomain("https://v4000rc2sdk.datarangers-onpremise.volces.com", null)
        config.isAbEnable = true
        config.logger =
            ILogger { s, throwable -> Log.d("AppLog------->: ", "" + s) }
        config.isAutoTrackEnabled = true //开启圈选预置事件开关，true开启，false关闭
        config.isH5CollectEnable = true //关闭内嵌H5页面的无埋点事件
//        config.enableDeferredALink()
        AppLog.setEncryptAndCompress(false)
        config.setAutoStart(true)
        AppLog.init(this, config)
    }

}