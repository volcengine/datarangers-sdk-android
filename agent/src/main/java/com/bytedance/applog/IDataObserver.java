// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * register，config，abconfig相关数据本地加载和server加载的回调通知
 * @author linguoqing
 */
public interface IDataObserver {
    /**
     * 本地的id数据加载结果通知
     * @param did device id
     * @param iid install id
     * @param ssid 数说id
     */
    void onIdLoaded(@NonNull String did, @NonNull String iid, @NonNull String ssid);

    /**
     * 通知注册结果，以及id变化情况
     * 仅主进程会被调用
     * @param changed 是否和本地缓存有所不同
     * @param newDid server返回新的device id
     * @param newIid server返回新install id
     * @param newSsid server返回新数说id
     */
    void onRemoteIdGet(boolean changed, @Nullable String oldDid, @NonNull String newDid,
                       @NonNull String oldIid, @NonNull String newIid, @NonNull String oldSsid, @NonNull String newSsid);

    /**
     * Config拉取数据，和本地数据对比有变化的通知
     * 仅主进程会被调用
     * @param changed 是否和本地缓存有所不同
     * @param config server返回新config内容
     */
    void onRemoteConfigGet(boolean changed, @Nullable JSONObject config);

    /**
     * server拉取AbConfig数据，和本地数据对比有变化的通知
     * 仅主进程会被调用
     * @param changed 是否和本地缓存有所不同
     * @param abConfig server返回新abConfig内容
     */
    void onRemoteAbConfigGet(boolean changed, @NonNull JSONObject abConfig);

    /**
     * 客户端ab实验曝光vid集合发生变化的通知
     * @param vids 客户端ab实验曝光vid集合
     * @param extVids 外部设置的external vid集合
     */
    void onAbVidsChange(@NonNull String vids, @NonNull String extVids);
}
