// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface ILogger {
    void log(@NonNull final String msg, @Nullable final Throwable t);
}
