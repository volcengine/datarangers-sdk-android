// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.exception

import android.util.Log
import com.bytedance.applog.util.TLog

/**
 * 自定义的Aassert处理器
 *
 * @author luodong.seu
 */
class AssertException : Throwable() {

    override fun printStackTrace() {
        val print = TLog.e("Assert failed.", cause)
        if (print) {
            return
        }
        // 未初始化之前打印
        try {
            Log.e(
                TLog.TAG,
                "AppLog assert failed: " + (cause?.message
                    ?: (if (stackTrace.isNotEmpty()) stackTrace[0].toString() else "function interrupt"))
            )
        } catch (e: Throwable) {

        }
    }

}