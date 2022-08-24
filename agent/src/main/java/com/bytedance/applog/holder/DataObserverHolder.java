// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.holder;

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
    public void onIdLoaded(String did, String iid, String ssid) {
        for (IDataObserver observer : mDataObserver) {
            observer.onIdLoaded(did, iid, ssid);
        }
    }

    @Override
    public void onRemoteIdGet(
            boolean changed,
            String oldDid,
            String newDid,
            String oldIid,
            String newIid,
            String oldSsid,
            String newSsid) {
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
    public void onRemoteAbConfigGet(boolean changed, JSONObject abConfig) {
        for (IDataObserver observer : mDataObserver) {
            observer.onRemoteAbConfigGet(changed, abConfig);
        }
    }

    @Override
    public void onAbVidsChange(final String vids, final String extVids) {
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
