// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.event;

import androidx.annotation.NonNull;

import org.json.JSONObject;

/**
 * 事件处理接口
 *
 * @author luodong.seu
 */
public interface IEventHandler {

    /** @return EventType | EventType */
    int acceptType();

    /**
     * 采集事件的回调，可在该接口中处理数据内容
     *
     * @param eventType EventType const values
     * @param eventName 事件名
     * @param properties 事件属性
     * @return EventPolicy
     */
    EventPolicy onReceive(int eventType, @NonNull String eventName, @NonNull JSONObject properties, @NonNull EventBasicData basisData);
}
