// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

/**
 * 初始化的值
 *
 * @param <T> 泛型
 */
public class InitValue<T> {

    private T value = null;
    private boolean hasValueSet = false;

    /** 设置值 */
    public void setValue(T newValue) {
        value = newValue;
        hasValueSet = true;
    }

    /**
     * 是否有值
     *
     * @return true: 设置过值
     */
    public boolean hasValue() {
        return hasValueSet;
    }

    public T getValue() {
        return value;
    }
}
