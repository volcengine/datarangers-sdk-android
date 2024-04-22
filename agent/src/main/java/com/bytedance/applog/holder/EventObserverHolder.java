// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.holder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.applog.IEventObserver;
import com.bytedance.applog.IPresetEventObserver;
import com.bytedance.applog.event.EventObserverImpl;

import org.json.JSONObject;

import java.util.concurrent.CopyOnWriteArraySet;

/** @author linguoqing */
public class EventObserverHolder implements IEventObserver, IPresetEventObserver {

    private final CopyOnWriteArraySet<EventObserverImpl> mEventObserver = new CopyOnWriteArraySet<>();

    public EventObserverHolder() {}

    @Override
    public void onEvent(
            @NonNull String category,
            @NonNull String tag,
            String label,
            long value,
            long extValue,
            String extJson) {
        for (IEventObserver iEventObserver : mEventObserver) {
            iEventObserver.onEvent(category, tag, label, value, extValue, extJson);
        }
    }

    @Override
    public void onEventV3(@NonNull String event, @Nullable JSONObject params) {
        for (IEventObserver iEventObserver : mEventObserver) {
            iEventObserver.onEventV3(event, params);
        }
    }

    public void addEventObserver(EventObserverImpl listener) {
        if (listener != null) {
            mEventObserver.add(listener);
        }
    }

    public void removeEventObserver(EventObserverImpl listener) {
        if (listener != null) {
            mEventObserver.remove(listener);
        }
    }

    @Override
    public void onPageEnter(JSONObject params) {
        for (IPresetEventObserver iPresetEventObserver : mEventObserver) {
            iPresetEventObserver.onPageEnter(params);
        }
    }

    @Override
    public void onPageLeave(JSONObject params) {
        for (IPresetEventObserver iPresetEventObserver : mEventObserver) {
            iPresetEventObserver.onPageLeave(params);
        }
    }

    @Override
    public void onLaunch(JSONObject params) {
        for (IPresetEventObserver iPresetEventObserver : mEventObserver) {
            iPresetEventObserver.onLaunch(params);
        }
    }
}
