// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.event;

import com.bytedance.applog.store.BaseData;

public class EventBasisParser {

    public static EventBasicData parseEvent(BaseData event) {
        return new EventBasicData(event);
    }
}
