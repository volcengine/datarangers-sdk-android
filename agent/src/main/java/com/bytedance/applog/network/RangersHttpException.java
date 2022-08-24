// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.network;

/**
 * 统一内部版和tob版 http code处理逻辑
 * Created by luoqiaoyou on 2020-01-14.
 */
public class RangersHttpException extends Exception {

    private int mResponseCode;

    public RangersHttpException(int status, String exceptionStr) {
        super(exceptionStr);
        mResponseCode = status;
    }

    public RangersHttpException(int status, Throwable tr) {
        super(tr);
        mResponseCode = status;
    }

    public int getResponseCode() {
        return mResponseCode;
    }
}
