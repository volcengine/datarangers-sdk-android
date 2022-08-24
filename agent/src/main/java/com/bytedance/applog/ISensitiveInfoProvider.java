// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

/**
 * 外部提供敏感数据
 * Created by luoqiaoyou on 2020/2/4.
 */
public interface ISensitiveInfoProvider {
    String getMacAddress();

    String getOAID();

    String[] getIMEI();

    String getAndroidId();

    String getBuildSerial();
}