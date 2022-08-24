// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import java.util.UUID;

public class RequestIdGenerator {
    private static AbsSingleton<String> requestIdSingleTon = new AbsSingleton<String>() {
        @Override
        protected String create(Object... params) {
            return UUID.randomUUID().toString();
        }
    };

    public static String getRequestId() {
        return requestIdSingleTon.get();
    }
}