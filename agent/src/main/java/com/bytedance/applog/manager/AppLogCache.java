// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.engine.Session;
import com.bytedance.applog.store.BaseData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * applog事件缓存，当未初始化时收到上报，都缓存到这里；初始化完成后，再处理。
 *
 * @author chenjian
 * @date 2017/7/28
 */
public class AppLogCache {

    private final LinkedList<BaseData> sDatas = new LinkedList<>();

    private final LinkedList<String> sStrings = new LinkedList<>();

    private final int LIMIT_EVENT_SIZE = 300;

    public void cache(BaseData data) {
        synchronized (sDatas) {
            if (sDatas.size() > LIMIT_EVENT_SIZE) {
                sDatas.poll();
            }
            sDatas.add(data);
        }
    }

    public void cache(String[] array) {
        synchronized (sStrings) {
            if (sStrings.size() > LIMIT_EVENT_SIZE) {
                sStrings.poll();
            }
            sStrings.addAll(Arrays.asList(array));
        }
    }

    public int dumpData(ArrayList<BaseData> data, AppLogInstance appLog, Session session) {
        synchronized (sDatas) {
            int size = sDatas.size();
            for (BaseData event : sDatas) {
                session.process(appLog, event, data);
                data.add(event);
            }
            sDatas.clear();
            return size;
        }
    }

    public String[] getArray() {
        final int count = sStrings.size();
        String[] array = null;
        if (count > 0) {
            array = new String[count];
            sStrings.toArray(array);
            sStrings.clear();
        }
        return array;
    }
}
