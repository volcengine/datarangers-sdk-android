// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.filter;

import com.bytedance.applog.BaseAppLogTest;
import java.util.HashMap;
import java.util.HashSet;
import org.junit.*;

/**
 * Created by lixiao on 2020/8/20.
 */
public class EventFilterTest extends BaseAppLogTest {
    @Test
    public void allowEventName() {
        HashSet<String> eventSet = new HashSet<String>();
        eventSet.add("test");
        AllowEventFilter filter = new AllowEventFilter(eventSet, new HashMap<String, HashSet<String>>());
        Assert.assertFalse(filter.filter("test1", ""));
        Assert.assertTrue(filter.filter("test", ""));
    }

    @Test
    public void blockEventParam() {
        HashSet<String> eventSet = new HashSet<String>();
        eventSet.add("test");
        BlockEventFilter filter = new BlockEventFilter(eventSet, new HashMap<String, HashSet<String>>());
        Assert.assertTrue(filter.filter("test1", ""));
        Assert.assertFalse(filter.filter("test", ""));
    }
}
