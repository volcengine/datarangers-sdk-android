// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import androidx.annotation.Nullable;

import org.json.JSONObject;

public interface IPickerCallback {
    void success(@Nullable JSONObject pageInfo);
    void failed(@Nullable String message);
}
