// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.network;

import java.net.HttpURLConnection;

/** Http超时错误 */
public class RangersHttpTimeoutException extends RangersHttpException {

    public RangersHttpTimeoutException(String exceptionStr) {
        super(HttpURLConnection.HTTP_CLIENT_TIMEOUT, exceptionStr);
    }
}
