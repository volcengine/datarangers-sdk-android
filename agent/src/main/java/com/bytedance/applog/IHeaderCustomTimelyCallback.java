// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import org.json.JSONObject;

// FIXME: 2020-03-11 这个接口，应该废弃。不应该由外部直接修改header，绕过AppLog的认知了。
public interface IHeaderCustomTimelyCallback {

    /**
     * applog上报时，实时修改header参数
     * <p>往header中添加相关字段，请先联系applog开发同学；applog sdk内部有header信息过滤逻辑，需要在sdk内注册header的key
     * @param headerToUpdate
     */
    void updateHeader(JSONObject headerToUpdate);

}
