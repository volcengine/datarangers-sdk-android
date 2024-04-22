// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.event;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.applog.IEventObserver;
import com.bytedance.applog.IPresetEventObserver;

import org.json.JSONObject;

public class EventObserverImpl implements IPresetEventObserver, IEventObserver {

    private final IEventObserver eventObserver;
    private final IPresetEventObserver presetEventObserver;

    private EventObserverImpl(IEventObserver eventObserver, IPresetEventObserver presetEventObserver) {
        this.eventObserver = eventObserver;
        this.presetEventObserver = presetEventObserver;
    }

    public static class EventFactory {

        public static EventObserverImpl creteEventObserver(IEventObserver eventObserver, IPresetEventObserver presetEventObserver) {
            return new EventObserverImpl(eventObserver, presetEventObserver);
        }
    }

    @Override
    public void onEvent(@NonNull String category, @NonNull String tag, String label, long value, long extValue, String extJson) {
        if (eventObserver == null) return;
        eventObserver.onEvent(category, tag, label, value, extValue, extJson);
    }

    @Override
    public void onEventV3(@NonNull String event, @Nullable JSONObject params) {
        if (eventObserver == null) return;
        eventObserver.onEventV3(event, params);
    }

    @Override
    public void onPageEnter(JSONObject params) {
        if (presetEventObserver == null) return;
        presetEventObserver.onPageEnter(params);
    }

    @Override
    public void onPageLeave(JSONObject params) {
        if (presetEventObserver == null) return;
        presetEventObserver.onPageLeave(params);
    }

    @Override
    public void onLaunch(JSONObject params) {
        if (presetEventObserver == null) return;
        presetEventObserver.onLaunch(params);
    }
}
