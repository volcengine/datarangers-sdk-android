// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class JsonUtils {

    public static boolean compareJsons(JSONObject json1, JSONObject json2, String key)
            throws JSONException {
        if (!commonCompare(json1, json2, key)) {
            return false;
        }
        if (json1 != null && json1.length() != json2.length()) {
            return false;
        }
        Iterator<String> json1Keys = json1.keys();
        boolean result = true;
        while (json1Keys.hasNext()) {
            key = json1Keys.next();
            result = compareJsons(json1.get(key), json2.get(key), key);
            if (!result) {
                break;
            }
        }
        return result;
    }

    private static boolean compareJsons(Object json1, Object json2, String key)
            throws JSONException {
        if (!commonCompare(json1, json2, key)) {
            return false;
        }
        if (json1 instanceof JSONObject) {
            return compareJsons((JSONObject) json1, (JSONObject) json2, key);
        } else if (json1 instanceof JSONArray) {
            return compareJsons((JSONArray) json1, (JSONArray) json2, key);
        } else {
            // if not the same class
            // e.g. like user = 123 not equals user="123"
            if (json1.getClass() != json2.getClass()) {
                return false;
            }
            // change to the strings and compare
            return compareJsons(json1.toString(), json2.toString(), key);
        }
    }

    // use map for diff the jsonarray
    private static boolean compareJsons(JSONArray jsonArray1, JSONArray jsonArray2, String key)
            throws JSONException {
        boolean commonCompare = commonCompare(jsonArray1, jsonArray2, key);
        if (!commonCompare) {
            return false;
        }
        HashMap<Object, Integer> map1 = new HashMap<>();
        for (int i = 0; i < jsonArray1.length(); i++) {
            Object o = jsonArray1.get(i);
            if (map1.containsKey(o) && map1.get(o) != null) {
                map1.put(o, map1.get(o) + 1);
            } else {
                map1.put(o, 1);
            }
        }

        HashMap<Object, Integer> map2 = new HashMap<>();
        for (int i = 0; i < jsonArray2.length(); i++) {
            Object o = jsonArray2.get(i);
            if (map2.containsKey(o) && map2.get(o) != null) {
                map2.put(o, map2.get(o) + 1);
            } else {
                map2.put(o, 1);
            }
        }

        // if the size of the maps are not equals , return false
        if (map1.size() != map2.size()) {
            return false;
        }

        // calc the count of each elements
        Set<Map.Entry<Object, Integer>> entries = map1.entrySet();
        for (Map.Entry<Object, Integer> entry : entries) {
            Object key1 = entry.getKey();
            Integer integer = map2.get(key1);
            if (!entry.getValue().equals(integer)) {
                return false;
            }
        }
        return true;
    }

    private static boolean compareJsons(String json1, String json2, String key) {
        boolean commonCompare = commonCompare(json1, json2, key);
        return commonCompare && json1.equals(json2);
    }

    private static boolean commonCompare(Object json1, Object json2, String key) {
        return (json1 != null || json2 == null) && (json1 == null || json2 != null);
    }

    public static boolean paramValueCheck(
            JSONObject jsonObject,
            @Nullable Class<?>[] checkTypeInValue,
            @Nullable Class<?>[] checkTypeInArray)
            throws JSONException {
        if (jsonObject == null) {
            return false;
        }
        Iterator<String> iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = jsonObject.get(key);
            if (value == null) {
                return false;
            }
            if (value instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) value;
                for (int i = 0; i < jsonArray.length(); i++) {
                    Object item = jsonArray.get(i);
                    if (checkTypeInArray != null
                            && !arrayContains(checkTypeInArray, item.getClass())) {
                        return false;
                    }
                }
            } else {
                if (checkTypeInValue != null
                        && !arrayContains(checkTypeInValue, value.getClass())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static <T> boolean arrayContains(T[] array, T value) {
        for (T t : array) {
            if (t == value) {
                return true;
            }
        }
        return false;
    }

    /**
     * 合并2个JSON
     *
     * @param from source json
     * @param to target json
     */
    @SuppressWarnings("UnusedReturnValue")
    public static JSONObject mergeJsonObject(JSONObject from, JSONObject to) {
        if (null == from) {
            return to;
        }
        Iterator<String> keys = from.keys();
        try {
            while (keys.hasNext()) {
                String key = keys.next();
                to.put(key, from.opt(key));
            }
        } catch (JSONException e) {
            TLog.e("Merge json interrupted.", e);
        }
        return to;
    }
}
