// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * Page、Launch 的预置事件回调。时机并不可靠
 */
public interface IPresetEventObserver {

    void onPageEnter(@Nullable JSONObject params);

    void onPageLeave(@Nullable JSONObject params);

    void onLaunch(@Nullable JSONObject params);
}
