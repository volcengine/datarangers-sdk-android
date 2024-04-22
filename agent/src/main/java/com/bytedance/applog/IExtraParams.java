// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;

/**
 * API请求时候，如果需要往uri中添加额外的参数，可以通过该接口设置
 * @author linguoqing
 */
public interface IExtraParams {
    /**
     * 提供额外的API请求uri参数
     * @param level
     * @return extraParams
     */
    @Nullable
    HashMap<String, String> getExtraParams(@NonNull Level level);
}
