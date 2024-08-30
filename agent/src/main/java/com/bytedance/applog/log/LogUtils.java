// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.log;

import com.bytedance.applog.store.BaseData;
import com.bytedance.applog.store.CustomEvent;
import com.bytedance.applog.store.EventV3;
import com.bytedance.applog.store.Launch;
import com.bytedance.applog.store.Page;
import com.bytedance.applog.store.Profile;
import com.bytedance.applog.store.Terminate;
import com.bytedance.applog.util.JsonUtils;
import com.bytedance.applog.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * 日志工具类
 *
 * @author luodong.seu
 */
public class LogUtils {
    public static final String EVENT_TYPE_LAUNCH = "LAUNCH";
    public static final String EVENT_TYPE_TERMINATE = "TERMINATE";
    public static final String EVENT_TYPE_EVENT_V3 = "EVENT_V3";
    public static final String EVENT_TYPE_PROFILE = "PROFILE";
    public static final String EVENT_TYPE_TRACE = "TRACE";
    private static volatile boolean buzEnabled = false;

    public static void setEnable(boolean enable) {
        buzEnabled = enable;
    }

    /**
     * 发送懒加载方式的数据
     *
     * @param event event key
     * @param fetcher data fetcher
     */
    public static void sendJsonFetcher(String event, EventBus.DataFetcher fetcher) {
        if (isDisabled() || Utils.isEmpty(event)) {
            return;
        }
        EventBus.global.get().emit(wrapEvent(event), fetcher);
    }

    /**
     * 发送字符串数据
     *
     * @param event event key
     * @param data 字符串
     */
    public static void sendString(String event, String data) {
        if (isDisabled() || Utils.isEmpty(event)) {
            return;
        }
        EventBus.global.get().emit(wrapEvent(event), data);
    }

    /**
     * 发送json
     *
     * @param event event key
     * @param data JSONObject
     */
    public static void sendJson(String event, JSONObject data) {
        if (isDisabled() || Utils.isEmpty(event)) {
            return;
        }
        EventBus.global.get().emit(wrapEvent(event), data);
    }

    /**
     * 发送object对象
     *
     * @param event event key
     * @param data JSONObject
     */
    public static void sendObject(String event, final Object data) {
        if (isDisabled() || Utils.isEmpty(event)) {
            return;
        }
        if (data instanceof BaseData) {
            EventBus.global
                    .get()
                    .emit(
                            wrapEvent(event),
                            new EventBus.DataFetcher() {
                                @Override
                                public Object fetch() {
                                    JSONObject json = ((BaseData) data).toPackJson();
                                    JSONObject copy = new JSONObject();
                                    JsonUtils.mergeJsonObject(json, copy);
                                    try {
                                        copy.put("$$APP_ID", ((BaseData) data).getAppId());
                                        copy.put("$$EVENT_TYPE", extractEventType((BaseData) data));
                                        copy.put(
                                                "$$EVENT_LOCAL_ID", ((BaseData) data).localEventId);
                                    } catch (JSONException ignored) {

                                    }
                                    return copy;
                                }
                            });
        } else {
            EventBus.global.get().emit(wrapEvent(event), data);
        }
    }

    private static String wrapEvent(String event) {
        return "applog_" + event;
    }

    /** 从basedata中获取事件类型 */
    private static String extractEventType(BaseData data) {
        if (null == data) {
            return "";
        }
        if (data instanceof EventV3 || data instanceof Page) {
            return EVENT_TYPE_EVENT_V3;
        }
        if (data instanceof CustomEvent) {
            return ((CustomEvent) data).getCategory().toUpperCase(Locale.ROOT);
        }
        if (data instanceof Launch) {
            return EVENT_TYPE_LAUNCH;
        }
        if (data instanceof Terminate) {
            return EVENT_TYPE_TERMINATE;
        }
        if (data instanceof Profile) {
            return EVENT_TYPE_PROFILE;
        }
        return "";
    }

    /**
     * 是否禁用
     *
     * @return true:被禁用
     */
    public static boolean isDisabled() {
        return !buzEnabled;
    }
}
