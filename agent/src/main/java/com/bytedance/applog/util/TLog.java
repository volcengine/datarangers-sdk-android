// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import com.bytedance.applog.BuildConfig;

/**
 * @author shiyanlong
 * @date 2019/1/16
 */
public class TLog {

    public static final String SDK_VERSION_NAME = BuildConfig.VERSION_NAME;
    public static final int SDK_VERSION = BuildConfig.VERSION_CODE;
    public static final int SDK_VERSION_CODE = 9999999 + (SDK_VERSION - 400);

    /** 添加日志接口，优化日志打印，无须打印时不执行函数 */
    public interface LogGetter {
        String log();
    }
}
