// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

public abstract class AbsSingleton<T> {
    private volatile T mInstance;

    protected abstract T create(Object... params);

    public final T get(Object... params) {
        if (mInstance == null) {
            synchronized (this) {
                if (mInstance == null) {
                    mInstance = create(params);
                }
            }
        }
        return mInstance;
    }
}