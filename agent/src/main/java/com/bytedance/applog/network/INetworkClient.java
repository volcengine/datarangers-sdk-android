// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.network;

import org.json.JSONObject;

import java.util.Map;

/**
 * @author linguoqing
 */
public interface INetworkClient {

    byte METHOD_GET = 0;
    byte METHOD_POST = 1;
    byte RESPONSE_TYPE_STRING = 0;
    byte RESPONSE_TYPE_STREAM = 1;

    /**
     * 网络客户端接口
     *
     * @param method         INetworkClient.METHOD_GET | INetworkClient.METHOD_POST
     * @param url            请求url
     * @param body           请求原始body内容
     * @param requestHeaders 请求头
     * @param responseType   响应类型 INetworkClient.RESPONSE_TYPE_STREAM | INetworkClient
     *                       .RESPONSE_TYPE_STRING
     * @param encrypt        是否加密
     * @param timeout        超时时长
     * @return 响应的字节数组 可以转String或key iv解密
     * @throws RangersHttpException
     */
    byte[] execute(
            final byte method,
            final String url,
            final JSONObject body,
            final Map<String, String> requestHeaders,
            final byte responseType,
            final boolean encrypt,
            final int timeout)
            throws RangersHttpException;
}
