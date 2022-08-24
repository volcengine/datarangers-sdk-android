// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.network;

import android.util.Pair;

import java.util.List;
import java.util.Map;

/**
 * @author linguoqing
 */
public interface INetworkClient {
    /**
     * get接口，实现get请求
     * @param url
     * @param requestHeaders
     * @return
     * @throws Exception
     */
    String get(final String url, final Map<String, String> requestHeaders) throws RangersHttpException;

    /**
     * post接口，实现post请求
     * @param url
     * @param data
     * @param requestHeaders
     * @return
     * @throws Exception
     */
    String post(final String url, final byte[] data, final Map<String, String> requestHeaders) throws RangersHttpException;

    /**
     * 带contentType的post请求
     * @param url
     * @param bytes
     * @param contentType
     * @return
     * @throws Exception
     */
    String post(String url, byte[] bytes, String contentType) throws RangersHttpException;

    /**
     * 带pair params的post请求
     * @param url
     * @param params
     * @return
     * @throws Exception
     */
    String post(String url, List<Pair<String, String>> params) throws RangersHttpException;

    /**
     * post method, return byte array
     * @param url
     * @param data
     * @param requestHeaders
     * @return
     * @throws Exception
     */
    byte[] postStream(final String url, final byte[] data, final Map<String, String> requestHeaders) throws RangersHttpException;
}
