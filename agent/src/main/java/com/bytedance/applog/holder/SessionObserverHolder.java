// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.holder;

import com.bytedance.applog.ISessionObserver;

import org.json.JSONObject;

import java.util.concurrent.CopyOnWriteArraySet;

/** @author linguoqing */
public class SessionObserverHolder implements ISessionObserver {

    private final CopyOnWriteArraySet<ISessionObserver> mSessionObserver =
            new CopyOnWriteArraySet<>();

    public SessionObserverHolder() {}

    @Override
    public void onSessionStart(long id, String sessionId) {
        for (ISessionObserver iSessionObserver : mSessionObserver) {
            iSessionObserver.onSessionStart(id, sessionId);
        }
    }

    @Override
    public void onSessionTerminate(long id, String sessionId, JSONObject appLog) {
        for (ISessionObserver iSessionObserver : mSessionObserver) {
            iSessionObserver.onSessionTerminate(id, sessionId, appLog);
        }
    }

    @Override
    public void onSessionBatchEvent(long id, String sessionId, JSONObject appLog) {
        for (ISessionObserver iSessionObserver : mSessionObserver) {
            iSessionObserver.onSessionBatchEvent(id, sessionId, appLog);
        }
    }

    public void addSessionHook(ISessionObserver listener) {
        if (listener != null) {
            mSessionObserver.add(listener);
        }
    }

    public void removeSessionHook(ISessionObserver listener) {
        if (listener != null) {
            mSessionObserver.remove(listener);
        }
    }

    public int getObserverSize() {
        return mSessionObserver.size();
    }

    public void removeAllSessionHook() {
        mSessionObserver.clear();
    }
}
