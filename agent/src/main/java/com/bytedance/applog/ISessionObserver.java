// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import org.json.JSONObject;

/** 对外提供监听，通知session开始、结束和“批量打包”事件。 这个调用不是实时的，相当于仅对外暴露了launch、terminate和applog数据。 */
public interface ISessionObserver {
    /**
     * 仅通知session start事件
     *
     * @param id 递增id
     * @param sessionId
     */
    void onSessionStart(long id, String sessionId);

    /**
     * 仅通知session terminate事件，对appLog数据的修改都会忽略
     *
     * @param id 递增id
     * @param sessionId
     * @param appLog
     */
    void onSessionTerminate(long id, String sessionId, JSONObject appLog);

    /**
     * 通知sdk对事件批量处理 主端通过该回调把"item_impression"设置到sdk，所以sdk会在该回调后检查是否有"item_impression"数据，有的话会处理该字段
     * 其他对appLog修改无效，sdk将忽略
     *
     * @param id 递增id
     * @param sessionId
     * @param appLog
     */
    void onSessionBatchEvent(long id, String sessionId, JSONObject appLog);
}
