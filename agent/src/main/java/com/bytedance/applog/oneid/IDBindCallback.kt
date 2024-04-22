// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.oneid

import androidx.annotation.Keep

/**
 *
 * @author: baoyongzhang
 * @date: 2022/10/9
 */
@Keep
interface IDBindCallback {
    fun onSuccess(result: IDBindResult?)
    fun onFail(code: Int, message: String?)
}

@Keep
data class IDBindResult(val newSsid: String?, val failedIdList: String?)