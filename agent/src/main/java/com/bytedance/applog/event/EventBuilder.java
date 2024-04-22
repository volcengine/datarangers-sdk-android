// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.event;

import static com.bytedance.applog.AppLogInstance.DEFAULT_EVENT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.log.LogInfo;
import com.bytedance.applog.store.EventV3;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 事件构造器
 *
 * @author: baoyongzhang
 * @date: 2022/9/29
 * @since 6.14.0
 */
public class EventBuilder {

    private final AppLogInstance instance;

    private String event;
    private String abSdkVersion;
    private JSONObject params;

    public EventBuilder(@NonNull AppLogInstance instance) {
        this.instance = instance;
    }

    public EventBuilder setEvent(@NonNull String event) {
        this.event = event;
        return this;
    }

    /**
     * 添加事件自定义 AB Vid，只有本事件会携带，会和全局的 abSdkVersion 做合并
     *
     * @param abSdkVersion
     * @return
     */
    public EventBuilder setAbSdkVersion(@Nullable String abSdkVersion) {
        this.abSdkVersion = abSdkVersion;
        return this;
    }

    /**
     * 添加事件属性
     *
     * @param name
     * @param value
     * @return
     */
    public EventBuilder addParam(@NonNull String name, @Nullable Object value) {
        if (params == null) {
            params = new JSONObject();
        }
        try {
            params.put(name, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * 构造事件对象
     *
     * @return
     */
    public EventV3 build() {
        EventV3 eventV3 = new EventV3(
                instance.getAppId(), event, false, params != null ? params.toString() : null, DEFAULT_EVENT);
        eventV3.abSdkVersion = abSdkVersion;

        instance.getLogger()
                .debug(LogInfo.Category.EVENT, "EventBuilder build: {}", eventV3);

        return eventV3;
    }

    /**
     * 直接发送事件
     */
    public void track() {
        EventV3 eventV3 = build();
        instance.getLogger()
                .debug(LogInfo.Category.EVENT, "EventBuilder track: " + event);
        instance.receive(eventV3);
    }

}
