// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

/**
 * @author shiyanlong
 * @date 2019/3/21
 **/
public interface IPicker {
    /**
     * 设置圈选登陆返回的cookie
     *
     * @param cookie
     */
    void setMarqueeCookie(String cookie);

    /**
     * 获取圈选cookie
     *
     * @return cookie
     */
    String getMarqueeCookie();
}
