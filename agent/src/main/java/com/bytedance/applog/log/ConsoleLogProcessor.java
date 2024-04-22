// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.log;

import android.util.Log;

import com.bytedance.applog.AppLogInstance;

/**
 * 控制台打印日志处理器
 *
 * @author luodong.seu
 */
public class ConsoleLogProcessor implements ILogProcessor {
    public static final String TAG = "AppLog";

    public ConsoleLogProcessor(AppLogInstance appLogInstance) {
        onLog(
                LogInfo.builder()
                        .appId(appLogInstance.getAppId())
                        .level(LogInfo.Level.DEBUG)
                        .thread(Thread.currentThread().getName())
                        .message("Console logger debug is:" + appLogInstance.isDebugMode())
                        .build());
    }

    @Override
    public void onLog(LogInfo log) {
        switch (log.getLevel()) {
            case LogInfo.Level.INFO:
                Log.i(TAG, log.toLiteString());
                break;
            case LogInfo.Level.WARNING:
                Log.w(TAG, log.toLiteString(), log.getThrowable());
                break;
            case LogInfo.Level.ERROR:
            case LogInfo.Level.ASSERT:
                Log.e(TAG, log.toLiteString(), log.getThrowable());
                break;
            case LogInfo.Level.DEBUG:
            default:
                Log.d(TAG, log.toLiteString());
                break;
        }
    }
}
