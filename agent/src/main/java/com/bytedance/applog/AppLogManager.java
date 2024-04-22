// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import androidx.annotation.Nullable;

import com.bytedance.applog.util.Utils;

/**
 * 公共的AppLog管理工具类
 *
 * @author luodong.seu
 */
public final class AppLogManager {

    /**
     * 通过AppId获取实例
     *
     * @param appId String
     * @return IAppLogInstance
     */
    public static @Nullable IAppLogInstance getInstance(String appId) {
        return Utils.isEmpty(appId) ? null : AppLogHelper.getInstanceByAppId(appId);
    }
}
