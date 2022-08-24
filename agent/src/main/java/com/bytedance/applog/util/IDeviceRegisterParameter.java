// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.accounts.Account;
import android.os.Handler;

import org.json.JSONArray;

/** Create by wusicheng 2019/6/2 */
public interface IDeviceRegisterParameter {
    String getUdId();

    JSONArray getUdIdList();

    String[] getSimSerialNumbers();

    String getOpenUdid();

    String getClientUDID();

    String getSerialNumber();

    String getDeviceId();

    void updateDeviceId(String deviceId);

    void setAccount(Account account);

    void clear(String clearKey);

    void setCacheHandler(Handler mWorkHandler);
}
