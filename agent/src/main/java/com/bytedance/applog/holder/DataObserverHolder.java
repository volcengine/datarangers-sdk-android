// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.holder;

import androidx.annotation.NonNull;

import com.bytedance.applog.IDataObserver;

import org.json.JSONObject;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 数据变化通知接口
 *
 * @author linguoqing
 */
public class DataObserverHolder implements IDataObserver {

    private final CopyOnWriteArraySet<IDataObserver> mDataObserver = new CopyOnWriteArraySet<>();

    public DataObserverHolder() {}

    @Override
    public void onIdLoaded(@NonNull String did, @NonNull String iid, @NonNull String ssid) {
        for (IDataObserver observer : mDataObserver) {
            observer.onIdLoaded(did, iid, ssid);
        }
    }

    @Override
    public void onRemoteIdGet(
            boolean changed,
            String oldDid,
            @NonNull String newDid,
            @NonNull String oldIid,
            @NonNull String newIid,
            @NonNull String oldSsid,
            @NonNull String newSsid) {
        for (IDataObserver observer : mDataObserver) {
            observer.onRemoteIdGet(changed, oldDid, newDid, oldIid, newIid, oldSsid, newSsid);
        }
    }

    @Override
    public void onRemoteConfigGet(boolean changed, JSONObject config) {
        for (IDataObserver observer : mDataObserver) {
            observer.onRemoteConfigGet(changed, config);
        }
    }

    @Override
    public void onRemoteAbConfigGet(boolean changed, @NonNull JSONObject abConfig) {
        for (IDataObserver observer : mDataObserver) {
            observer.onRemoteAbConfigGet(changed, abConfig);
        }
    }

    @Override
    public void onAbVidsChange(@NonNull final String vids, @NonNull final String extVids) {
        for (IDataObserver observer : mDataObserver) {
            observer.onAbVidsChange(vids, extVids);
        }
    }

    public void addDataObserver(IDataObserver listener) {
        if (listener != null) {
            mDataObserver.add(listener);
        }
    }

    public void removeDataObserver(IDataObserver listener) {
        if (listener != null) {
            mDataObserver.remove(listener);
        }
    }

    public void removeAllDataObserver() {
        mDataObserver.clear();
    }
}
