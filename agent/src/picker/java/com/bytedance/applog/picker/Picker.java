// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.picker;

import android.app.Application;

import com.bytedance.applog.IPicker;
import com.bytedance.applog.InitConfig;

/**
 * @author shiyanlong
 * @date 2019/1/16
 **/
public class Picker implements IPicker {
    /**
     * 圈选登陆返回的cookie
     */
    private String mMarqueeCookie;

    @Override
    public void setMarqueeCookie(String cookie) {
        mMarqueeCookie = cookie;
    }

    @Override
    public String getMarqueeCookie() {
        return mMarqueeCookie;
    }

    public Picker(final Application app, final InitConfig config) {

    }
}
