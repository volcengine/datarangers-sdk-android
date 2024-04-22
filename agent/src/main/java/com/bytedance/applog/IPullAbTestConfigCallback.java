// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * 拉取AB实验配置的回调接口
 *
 * @author luodong.seu
 */
public interface IPullAbTestConfigCallback {

    /**
     * 拉取远程配置成功
     *
     * @param config 实验JSON
     */
    void onRemoteConfig(@Nullable JSONObject config);

    /** 超时 */
    void onTimeoutError();

    /**
     * 限流回调
     *
     * @param remainingTime 剩余时间
     */
    void onThrottle(long remainingTime);
}
