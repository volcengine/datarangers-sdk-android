// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.bytedance.applog.engine.Engine;
import com.bytedance.applog.log.IAppLogLogger;
import com.bytedance.applog.store.BaseData;
import com.bytedance.applog.store.CustomEvent;
import com.bytedance.applog.store.EventV3;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class AppLogEventFilterConfig {


    public static final String BLOCK_LIST_KEY = "blocklist";
    public static final String WHITE_LIST_KEY = "whitelist";

    private static final String KEY_BLOCK_EVENTS_V1 = "block_events_v1";
    private static final String KEY_BLOCK_EVENTS_V3 = "block_events_v3";

    private static final String KEY_WHITE_EVENTS_V1 = "white_events_v1";
    private static final String KEY_WHITE_EVENTS_V3 = "white_events_v3";

    private static final String EVENT_LIST_V1 = "v1";
    private static final String EVENT_LIST_V3 = "v3";


    private final Set<String> blockSetV1 = new HashSet<>();
    private final Set<String> blockSetV3 = new HashSet<>();

    private final Set<String> whiteSetV1 = new HashSet<>();
    private final Set<String> whiteSetV3 = new HashSet<>();

    private final SharedPreferences eventConfigSp;
    private final IAppLogLogger logger;

    public AppLogEventFilterConfig(SharedPreferences sp, IAppLogLogger logger) {
        eventConfigSp = sp;
        this.logger = logger;
        // 读取缓存中的 events config
        Set<String> cachedBlockV1Events = eventConfigSp.getStringSet(KEY_BLOCK_EVENTS_V1, null);
        if (null != cachedBlockV1Events) {
            blockSetV1.addAll(cachedBlockV1Events);
        }
        Set<String> cachedBlockV3Events = eventConfigSp.getStringSet(KEY_BLOCK_EVENTS_V3, null);
        if (null != cachedBlockV3Events) {
            blockSetV3.addAll(cachedBlockV3Events);
        }

        Set<String> cachedWhiteV1Events = eventConfigSp.getStringSet(KEY_WHITE_EVENTS_V1, null);
        if (null != cachedWhiteV1Events) {
            whiteSetV1.addAll(cachedWhiteV1Events);
        }
        Set<String> cachedWhiteV3Events = eventConfigSp.getStringSet(KEY_WHITE_EVENTS_V3, null);
        if (null != cachedWhiteV3Events) {
            whiteSetV3.addAll(cachedWhiteV3Events);
        }
    }


    private void parseEventList(JSONObject o, String filedKey) {
        JSONObject parseList = o.optJSONObject(filedKey);
        logger.debug("[AppLogEventFilterConfig] parseEventList filedKey -> " + filedKey);
        if (parseList != null) {
            JSONArray v1 = parseList.optJSONArray(EVENT_LIST_V1);
            logger.debug("[AppLogEventFilterConfig] parseEventList v1 -> " + v1);
            int countV1 = v1 != null ? v1.length() : 0;
            HashSet<String> parseSetV1 = new HashSet<>(countV1);
            for (int i = 0; i < countV1; ++i) {
                String parseName = v1.optString(i, null);
                if (!TextUtils.isEmpty(parseName)) {
                    parseSetV1.add(parseName);
                }
            }

            JSONArray v3 = parseList.optJSONArray(EVENT_LIST_V3);
            logger.debug("[AppLogEventFilterConfig] parseEventList v3 -> " + v3);
            int countV3 = v3 != null ? v3.length() : 0;
            HashSet<String> parseSetV3 = new HashSet<>(countV3);
            for (int i = 0; i < countV3; ++i) {
                String parseName = v3.optString(i, null);
                if (!TextUtils.isEmpty(parseName)) {
                    parseSetV3.add(parseName);
                }
            }
            if (BLOCK_LIST_KEY.equals(filedKey)) {
                updateEventFilterConfig(blockSetV1, parseSetV1, blockSetV3, parseSetV3, KEY_BLOCK_EVENTS_V1, KEY_BLOCK_EVENTS_V3);
            } else if (WHITE_LIST_KEY.equals(filedKey)) {
                updateEventFilterConfig(whiteSetV1, parseSetV1, whiteSetV3, parseSetV3, KEY_WHITE_EVENTS_V1, KEY_WHITE_EVENTS_V3);
            }
        } else {
            // 配置名单清空了 本地缓存也要清空 避免为空的本地设置过不会清空
            if (BLOCK_LIST_KEY.equals(filedKey)) {
                updateEventFilterConfig(blockSetV1, null, blockSetV3, null, KEY_BLOCK_EVENTS_V1, KEY_BLOCK_EVENTS_V3);
            } else if (WHITE_LIST_KEY.equals(filedKey)) {
                updateEventFilterConfig(whiteSetV1, null, whiteSetV3, null, KEY_WHITE_EVENTS_V1, KEY_WHITE_EVENTS_V3);
            }
        }
    }

    public void updateEventFilterConfig(Set<String> rawV1, Set<String> newV1, Set<String> rawV3,
                                        Set<String> newV3, String v1SpKey, String v3SpKey) {
        rawV1.clear();
        rawV3.clear();
        if (newV1 != null) {
            rawV1.addAll(newV1);
        }
        eventConfigSp.edit().putStringSet(v1SpKey, rawV1).apply();
        if (newV3 != null) {
            rawV3.addAll(newV3);
        }
        eventConfigSp.edit().putStringSet(v3SpKey, rawV3).apply();
    }


    public void filterBlockAndWhite(List<BaseData> datas, Engine engine) {
        filterBlock(datas, engine);
        filterWhite(datas);
    }

    /**
     * filter block list events
     */
    public void filterBlock(final List<BaseData> datas, Engine engine) {
        if (datas == null || datas.size() == 0) {
            return;
        }
        if (blockSetV1.isEmpty() && blockSetV3.isEmpty()) {
            return;
        }
        Iterator<BaseData> iterator = datas.iterator();
        while (iterator.hasNext()) {
            BaseData event = iterator.next();
            if (event instanceof EventV3) {
                EventV3 eventV3 = (EventV3) event;
                if (blockSetV3.contains(eventV3.getEvent())) {
                    iterator.remove();
                    logger.debug("[AppLogEventFilterConfig] filterBlock remove v3 -> " + event);
                }
            } else if (event instanceof CustomEvent) {
                JSONObject eventV1 = event.toPackJson();
                String eventName =
                        eventV1.optString("tag")
                                + (!TextUtils.isEmpty(eventV1.optString("label"))
                                ? eventV1.optString("label")
                                : "");
                if (blockSetV1.contains(eventName)) {
                    iterator.remove();
                    logger.debug("[AppLogEventFilterConfig] filterBlock remove b1 -> " + event);
                }
            }
        }
    }

    /**
     * filter white list events
     */
    private void filterWhite(List<BaseData> datas) {
        if (datas == null || datas.size() == 0) {
            return;
        }
        if (whiteSetV1.isEmpty() && whiteSetV3.isEmpty()) {
            return;
        }
        Iterator<BaseData> iterator = datas.iterator();
        while (iterator.hasNext()) {
            BaseData event = iterator.next();
            if (event instanceof EventV3) {
                EventV3 eventV3 = (EventV3) event;
                if (!whiteSetV3.contains(eventV3.getEvent())) {
                    iterator.remove();
                    logger.debug("[AppLogEventFilterConfig] filterWhite remove v3 -> " + event);
                }
            } else if (event instanceof CustomEvent) {
                JSONObject eventV1 = event.toPackJson();
                String eventName =
                        eventV1.optString("tag")
                                + (!TextUtils.isEmpty(eventV1.optString("label"))
                                ? eventV1.optString("label")
                                : "");
                if (!whiteSetV1.contains(eventName)) {
                    iterator.remove();
                    logger.debug("[AppLogEventFilterConfig] filterWhite remove b1 -> " + event);
                }
            }
        }
    }

    public void parseEventConfigList(JSONObject o) {
        parseEventList(o, BLOCK_LIST_KEY);
        parseEventList(o, WHITE_LIST_KEY);
    }
}
