// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * 全局事件回调。仅对外通知，不可修改event。
 * 外部上报点太多、分散，不好统一检测。所以在这里加了一个卡口。
 */
public interface IEventObserver {
    /**
     * 回调V1事件入库
     * @param category
     * @param tag
     * @param label
     * @param value
     * @param extValue
     * @param extJson
     */
    void onEvent(@NonNull final String category, @NonNull final String tag, final String label,
                 final long value, final long extValue, String extJson);

    /**
     * 回调V3事件入库
     * @param event
     * @param params
     */
    void onEventV3(@NonNull final String event, @Nullable final JSONObject params);
}
