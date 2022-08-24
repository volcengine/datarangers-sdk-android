// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.filter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import com.bytedance.applog.store.SharedPreferenceCacheHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/** @author yezhekai */
public abstract class AbstractEventFilter {

    private static final String KEY_EVENT_LIST = "event_list";
    private static final String KEY_IS_BLOCK = "is_block";
    private static final String KEY_EVENTS = "events";
    private static final String KEY_PARAMS = "params";

    public static final String SP_FILTER_NAME = "sp_filter_name";

    protected HashSet<String> mEventSet;
    protected HashMap<String, HashSet<String>> mParamMap;

    protected AbstractEventFilter(
            HashSet<String> eventSet, HashMap<String, HashSet<String>> paramMap) {
        mEventSet = eventSet;
        mParamMap = paramMap;
    }

    protected abstract boolean interceptEventName(String eventName);

    protected abstract boolean interceptEventParam(HashSet<String> filterParamSet, String param);

    /**
     * event filter(block or allow) for v3 event
     *
     * @param eventName v3 event name
     * @param params v3 event params
     * @return true if can report or false if can not report this event after filter
     */
    public final boolean filter(String eventName, String params) {
        if (TextUtils.isEmpty(eventName)) {
            return false;
        }
        if (mEventSet == null || mEventSet.size() <= 0) {
            return true;
        }
        if (interceptEventName(eventName)) {
            return false;
        }
        if (TextUtils.isEmpty(params)) {
            return true;
        }
        JSONObject paramJson = null;
        try {
            paramJson = new JSONObject(params);
        } catch (JSONException e) {
        }
        if (paramJson == null
                || mParamMap == null
                || mParamMap.size() <= 0
                || !mParamMap.containsKey(eventName)) {
            return true;
        }
        HashSet<String> paramSet = mParamMap.get(eventName);
        if (paramSet == null || paramSet.size() <= 0) {
            return true;
        }
        Iterator<String> jsonParamIt = paramJson.keys();
        while (jsonParamIt.hasNext()) {
            String jsonParam = jsonParamIt.next();
            if (interceptEventParam(paramSet, jsonParam)) {
                try {
                    jsonParamIt.remove();
                } catch (Throwable e) {
                }
            }
        }
        return true;
    }

    public static final AbstractEventFilter parseFilterFromServer(
            Context context, String spName, JSONObject logSettingJson) {
        AbstractEventFilter eventFilter = null;
        try {
            Editor editor =
                    SharedPreferenceCacheHelper.getSafeSharedPreferences(
                                    context, spName, Context.MODE_PRIVATE)
                            .edit();
            editor.clear().commit();
            if (logSettingJson == null || !logSettingJson.has(KEY_EVENT_LIST)) {
                return null;
            }
            JSONObject filterJson = logSettingJson.optJSONObject(KEY_EVENT_LIST);
            if (filterJson == null) {
                return null;
            }
            int isBlock = filterJson.optInt(KEY_IS_BLOCK, 0);
            editor.putInt(KEY_IS_BLOCK, isBlock);

            HashSet<String> eventSet = new HashSet<String>();
            JSONArray eventJsonArray = filterJson.optJSONArray(KEY_EVENTS);
            if (eventJsonArray != null && eventJsonArray.length() > 0) {
                for (int i = 0; i < eventJsonArray.length(); i++) {
                    String event = eventJsonArray.optString(i);
                    if (!TextUtils.isEmpty(event)) {
                        eventSet.add(event);
                    }
                }
            }
            if (eventSet.size() > 0) {
                editor.putStringSet(KEY_EVENTS, eventSet);
            }

            HashMap<String, HashSet<String>> paramMap = new HashMap<String, HashSet<String>>();
            JSONObject paramJson = filterJson.optJSONObject(KEY_PARAMS);
            if (paramJson != null) {
                Iterator<String> keys = paramJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (TextUtils.isEmpty(key)) {
                        continue;
                    }
                    HashSet<String> paramSet = new HashSet<String>();
                    JSONArray paramJsonArray = paramJson.optJSONArray(key);
                    if (paramJsonArray != null && paramJsonArray.length() > 0) {
                        for (int i = 0; i < paramJsonArray.length(); i++) {
                            String param = paramJsonArray.optString(i);
                            if (!TextUtils.isEmpty(param)) {
                                paramSet.add(param);
                            }
                        }
                    }
                    if (paramSet.size() > 0) {
                        paramMap.put(key, paramSet);
                    }
                }
            }
            if (paramMap.size() > 0) {
                for (Entry<String, HashSet<String>> entry : paramMap.entrySet()) {
                    editor.putStringSet(entry.getKey(), entry.getValue());
                }
            }

            editor.commit();

            if (isBlock > 0) {
                eventFilter = new BlockEventFilter(eventSet, paramMap);
            } else {
                eventFilter = new AllowEventFilter(eventSet, paramMap);
            }
        } catch (Throwable e) {
        }
        return eventFilter;
    }

    public static final AbstractEventFilter parseFilterFromLocal(Context context, String spName) {
        AbstractEventFilter eventFilter = null;
        try {
            SharedPreferences sp =
                    SharedPreferenceCacheHelper.getSafeSharedPreferences(
                            context, spName, Context.MODE_PRIVATE);
            int isBlock = 0;
            HashSet<String> eventSet = new HashSet<String>();
            HashMap<String, HashSet<String>> paramMap = new HashMap<String, HashSet<String>>();
            Map<String, ?> eventMap = null;
            try {
                eventMap = sp.getAll();
            } catch (Throwable e) {
            }
            if (eventMap == null || eventMap.size() <= 0) {
                return null;
            }
            for (Entry<String, ?> entry : eventMap.entrySet()) {
                if (entry == null) {
                    continue;
                }
                String key = entry.getKey();
                if (KEY_IS_BLOCK.equals(key)) {
                    isBlock = sp.getInt(KEY_IS_BLOCK, 0);
                } else if (KEY_EVENTS.equals(key)) {
                    Set<String> spEventSet = null;
                    try {
                        spEventSet = (Set<String>) entry.getValue();
                    } catch (Throwable e) {
                    }
                    if (spEventSet != null && spEventSet.size() > 0) {
                        eventSet.addAll(spEventSet);
                    }
                } else if (!TextUtils.isEmpty(key)) {
                    HashSet<String> paramSet = new HashSet<String>();
                    Set<String> spParamSet = null;
                    try {
                        spParamSet = (Set<String>) entry.getValue();
                    } catch (Throwable e) {
                    }
                    if (spParamSet != null && spParamSet.size() > 0) {
                        paramSet.addAll(spParamSet);
                    }
                    if (paramSet.size() > 0) {
                        paramMap.put(key, paramSet);
                    }
                }
            }

            if (isBlock > 0) {
                eventFilter = new BlockEventFilter(eventSet, paramMap);
            } else {
                eventFilter = new AllowEventFilter(eventSet, paramMap);
            }
        } catch (Throwable e) {
        }
        return eventFilter;
    }

    public static final AbstractEventFilter parseFilterFromClient(
            List<String> eventList, boolean isBlock) {
        if (eventList != null && !eventList.isEmpty()) {
            HashSet<String> eventSet = new HashSet<>();
            for (String event : eventList) {
                if (!TextUtils.isEmpty(event)) {
                    eventSet.add(event);
                }
            }
            if (!eventSet.isEmpty()) {
                if (isBlock) {
                    return new BlockEventFilter(eventSet, null);
                } else {
                    return new AllowEventFilter(eventSet, null);
                }
            }
        }
        return null;
    }
}
