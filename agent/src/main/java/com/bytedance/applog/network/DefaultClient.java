// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.network;

import com.bytedance.applog.server.Api;

import org.json.JSONObject;

import java.util.Map;

/**
 * @author linguoqing
 */
public class DefaultClient implements INetworkClient {

    private final Api api;

    public DefaultClient(final Api api) {
        this.api = api;
    }

    @Override
    public byte[] execute(
            byte method,
            String url,
            JSONObject body,
            Map<String, String> requestHeaders,
            byte responseType,
            boolean encrypt,
            int timeout)
            throws RangersHttpException {
        return api.httpRequestInner(
                method, url, requestHeaders, body, encrypt, responseType, timeout);
    }
}
