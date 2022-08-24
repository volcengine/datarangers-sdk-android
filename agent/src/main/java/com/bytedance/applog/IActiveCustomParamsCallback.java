// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import java.util.Map;

/**
 * 自定义的激活参数回调接口
 * @author luodong.seu
 */
public interface IActiveCustomParamsCallback {

    /**
     * 获取参数接口
     *
     * @return Map
     */
    Map<String, String> getParams();
}
