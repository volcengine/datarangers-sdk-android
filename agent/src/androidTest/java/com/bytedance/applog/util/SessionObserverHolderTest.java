// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.util.Log;

import com.bytedance.applog.ISessionObserver;
import com.bytedance.applog.holder.SessionObserverHolder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

/** 测试XXXHolder的weakReference回收和ConcurrentModification问题 */
public class SessionObserverHolderTest {

    private static SessionObserverHolder sSessionObserverHolder =
            SessionObserverHolder.getInstance();

    private volatile boolean mRunning = true;

    //    @Test
    public void process() {
        // 线程一直发送回调
        new EventNotifyThread().start();

        ArrayList<EventTestThread> threadArrayList = new ArrayList<>(20);
        for (int i = 0; i < 20; i++) {
            EventTestThread t = new EventTestThread();
            t.start();
            threadArrayList.add(t);
        }

        // 线程一直发送回调，直到注册回调的线程都跑完
        for (int i = 0; i < 20; i++) {
            try {
                threadArrayList.get(i).join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mRunning = false;

        // 20个线程，每个线程循环10次，每次：添加Thread自身作为Observer，然后移除。再添加一个不被引用的observer
        // GC前应该有200个
        Log.d("test", "=========before gc:" + sSessionObserverHolder.getObserverSize());
        Assert.assertTrue(sSessionObserverHolder.getObserverSize() == 200);

        // 释放引用
        for (EventTestThread thread : threadArrayList) {
            thread.clear();
        }

        threadArrayList.clear();
        while (sSessionObserverHolder.getObserverSize() != 0) {
            try {
                // 帮助触发GC
                Log.d("test", "" + new byte[1024 * 1024 * 10]);
                System.gc();
                Thread.sleep(200);
                Log.d("test", "=========in while:" + sSessionObserverHolder.getObserverSize());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // GC后，应该都被回收
        Log.d("test", "=========afeter gc:" + sSessionObserverHolder.getObserverSize());
        Assert.assertTrue(sSessionObserverHolder.getObserverSize() == 0);
    }

    /** 模拟业务侧注册回调 */
    public static class EventTestThread extends Thread {
        // 防回收
        private ArrayList<NonRefObserver> observers = new ArrayList<>(20);

        @Override
        public void run() {
            for (int i = 0; i < 10; i++) {
                NonRefObserver refObserver = new NonRefObserver();
                sSessionObserverHolder.addSessionHook(refObserver);
                observers.add(refObserver);
                try {
                    Thread.sleep(new Random().nextInt(100));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sSessionObserverHolder.removeSessionHook(refObserver);
                refObserver = new NonRefObserver();
                sSessionObserverHolder.addSessionHook(refObserver);
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
                    sSessionObserverHolder.onSessionStart(1, "123");
                    sSessionObserverHolder.onSessionBatchEvent(1, "234", jsonObject);
                    sSessionObserverHolder.onSessionTerminate(1, "234", jsonObject);
                    Thread.sleep(new Random().nextInt(100));
                } catch (InterruptedException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /** 模拟业务侧回调实现 */
    public static class NonRefObserver implements ISessionObserver {

        @Override
        public void onSessionStart(long id, String sessonId) {}

        @Override
        public void onSessionTerminate(long id, String sessonId, JSONObject appLog) {}

        @Override
        public void onSessionBatchEvent(long id, String sessonId, JSONObject appLog) {}
    }
}
