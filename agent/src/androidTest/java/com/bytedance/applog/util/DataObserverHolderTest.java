// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import com.bytedance.applog.IDataObserver;
import com.bytedance.applog.holder.DataObserverHolder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

/** 测试XXXHolder的weakReference回收和ConcurrentModification问题 */
public class DataObserverHolderTest {
    private static DataObserverHolder sDataObserverHolder = DataObserverHolder.getInstance();

    private volatile boolean mRunning = true;

    /** 模拟业务侧注册回调 */
    public static class EventTestThread extends Thread {

        // 防回收
        private ArrayList<NonRefObserver> observers = new ArrayList<>(20);

        @Override
        public void run() {
            for (int i = 0; i < 10; i++) {
                NonRefObserver refObserver = new NonRefObserver();
                sDataObserverHolder.addDataObserver(refObserver);
                observers.add(refObserver);
                try {
                    Thread.sleep(new Random().nextInt(100));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sDataObserverHolder.removeDataObserver(refObserver);
                refObserver = new NonRefObserver();
                sDataObserverHolder.addDataObserver(refObserver);
                observers.add(refObserver);
            }
        }

        public void clear() {
            observers.clear();
        }
    }

    /** 模拟发送回调 */
    private class EventNotifyThread extends Thread {

        @Override
        public void run() {
            while (mRunning) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("test5", "test6");
                    sDataObserverHolder.onIdLoaded("123", "123", "123");
                    sDataObserverHolder.onRemoteIdGet(
                            true, "123", "234", "123", "123", "123", "123");
                    sDataObserverHolder.onRemoteConfigGet(true, jsonObject);
                    sDataObserverHolder.onRemoteAbConfigGet(true, jsonObject);
                    sDataObserverHolder.onAbVidsChange("123456", "654321");
                    Thread.sleep(new Random().nextInt(100));
                } catch (InterruptedException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /** 模拟业务侧回调实现 */
    public static class NonRefObserver implements IDataObserver {

        @Override
        public void onIdLoaded(final String did, final String iid, final String ssid) {}

        @Override
        public void onRemoteIdGet(
                final boolean changed,
                final String oldDid,
                final String newDid,
                final String oldIid,
                final String newIid,
                final String oldSsid,
                final String newSsid) {}

        @Override
        public void onRemoteConfigGet(final boolean changed, final JSONObject config) {}

        @Override
        public void onRemoteAbConfigGet(final boolean changed, final JSONObject abConfig) {}

        @Override
        public void onAbVidsChange(final String vids, final String extVids) {}
    }
}
