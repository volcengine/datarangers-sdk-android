// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

public interface ILogger {
    void log(final String msg, final Throwable t);
}
