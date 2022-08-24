// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.network;

import android.text.TextUtils;
import android.util.Pair;

import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.TLog;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author linguoqing
 */
public class DefaultClient implements INetworkClient {

    public static final String CONTENT_TYPE = "application/json; charset=utf-8";

    private final Api api;

    public DefaultClient(final Api api) {
        this.api = api;
    }

    @Override
    public String get(final String url, final Map<String, String> requestHeaders)
            throws RangersHttpException {
        return api.http(Api.METHOD_GET, url, (HashMap<String, String>) requestHeaders, null);
    }

    @Override
    public String post(
            final String url, final byte[] data, final Map<String, String> requestHeaders)
            throws RangersHttpException {
        return api.http(Api.METHOD_POST, url, (HashMap<String, String>) requestHeaders, data);
    }

    @Override
    public String post(String url, byte[] data, String contentType) throws RangersHttpException {
        Map<String, String> m = new HashMap<String, String>();
        if (!TextUtils.isEmpty(contentType)) {
            m.put("Content-Type", contentType);
        }
        return post(url, data, m);
    }

    @Override
    public String post(String url, List<Pair<String, String>> params) throws RangersHttpException {
        JSONObject body = new JSONObject();
        if (params != null) {
            try {
                for (Pair<String, String> p : params) {
                    body.put(p.first, p.second);
                }
            } catch (Throwable e) {
                TLog.e(e);
            }
        }
        return post(url, body.toString().getBytes(), CONTENT_TYPE);
    }

    @Override
    public byte[] postStream(
            final String url, final byte[] data, final Map<String, String> requestHeaders)
            throws RangersHttpException {
        return api.httpStream(Api.METHOD_POST, url, (HashMap<String, String>) requestHeaders, data);
    }
}
