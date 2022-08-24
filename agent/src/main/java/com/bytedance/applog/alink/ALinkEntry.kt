// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.alink

import android.app.Activity
import android.os.Bundle
import com.bytedance.applog.alink.util.LinkUtils

/**
 * 一个无视图Activity
 * @author chenguanzhong
 * @date 2021/1/28
 **/
class ALinkEntry : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        setTheme(android.R.style.Theme_NoDisplay) //设置为无视图activity（不会resume）
        finish()
    }

}