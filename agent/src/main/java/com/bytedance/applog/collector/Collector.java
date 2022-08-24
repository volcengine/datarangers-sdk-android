// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.collector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bytedance.applog.AppLogHelper;
import com.bytedance.applog.engine.Engine;
import com.bytedance.applog.util.TLog;

/**
 * 跨进程上报的入口类，负责收集子进程的上报事件，交给{@link Engine}处理。
 *
 * @author shiyanlong
 * @date 2019/1/16
 **/
public class Collector extends BroadcastReceiver {

    public static final String KEY_DATA = "K_DATA";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        String[] strings = intent.getStringArrayExtra(KEY_DATA);
        if (strings != null && strings.length > 0) {
            AppLogHelper.receive(strings);
        } else {
            TLog.ysnp(null);
        }
    }
}
