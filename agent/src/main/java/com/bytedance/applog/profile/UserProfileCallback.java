// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.profile;

public interface UserProfileCallback {

    int NO_NET = 0;
    int NET_ERROR = 1;
    int RESULT_ERROR = 2;
    /**
     * applog没有初始化或者deviceId还是空
     */
    int NOT_SATISFY = 3;
    /**
     * 请求受限，最快1分钟请求一次
     */
    int REQUEST_LIMIT = 4;

    void onSuccess();

    void onFail(int code);
}
