// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.filter;

import java.util.HashMap;
import java.util.HashSet;

/**
 * @author yezhekai
 */
public class BlockEventFilter extends AbstractEventFilter {

    public BlockEventFilter(final HashSet<String> eventSet,
            final HashMap<String, HashSet<String>> paramMap) {
        super(eventSet, paramMap);
    }

    @Override
    protected boolean interceptEventName(String eventName) {
        return mEventSet.contains(eventName);
    }

    @Override
    protected boolean interceptEventParam(HashSet<String> filterParamSet, String param) {
        return filterParamSet.contains(param);
    }
}
