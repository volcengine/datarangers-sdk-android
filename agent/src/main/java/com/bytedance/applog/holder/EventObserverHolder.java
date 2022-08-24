// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.holder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.applog.IEventObserver;

import org.json.JSONObject;

import java.util.concurrent.CopyOnWriteArraySet;

/** @author linguoqing */
public class EventObserverHolder implements IEventObserver {

    private final CopyOnWriteArraySet<IEventObserver> mEventObserver = new CopyOnWriteArraySet<>();

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

    public void addEventObserver(IEventObserver listener) {
        if (listener != null) {
            mEventObserver.add(listener);
        }
    }

    public void removeEventObserver(IEventObserver listener) {
        if (listener != null) {
            mEventObserver.remove(listener);
        }
    }

    public int getObserverSize() {
        return mEventObserver.size();
    }
}
