// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.exception;

/**
 * 应用Crash类型
 *
 * @author luodong.seu
 */
public class AppCrashType {

    public static final int ALL = Integer.MAX_VALUE;

    public static final int JAVA = 1;

    public static boolean hasCrashType(int typeOptions, int eventType) {
        return (typeOptions & eventType) != 0;
    }

    public static boolean hasJavaCrashType(int typeOptions) {
        return hasCrashType(typeOptions, JAVA);
    }
}
