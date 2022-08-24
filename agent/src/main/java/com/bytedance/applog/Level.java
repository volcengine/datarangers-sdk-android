// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

/**
 * we split common params to L0 & L1
 */
public enum Level {
    L0(0),
    L1(1);

    private final int mLevel;

    Level(int level) {
        mLevel = level;
    }

    public int value(){
        return mLevel;
    }
}